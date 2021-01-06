/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 The Play Remote Configuration Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.playrconf.provider;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import io.playrconf.sdk.AbstractProvider;
import io.playrconf.sdk.FileCfgObject;
import io.playrconf.sdk.KeyValueCfgObject;
import io.playrconf.sdk.exception.RemoteConfException;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.FS;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Retrieves configuration hosted behind a Git repository.
 * You can use three authentication modes:
 * 1. None     (Used for public repositories);
 * 2. User     (Used for private repositories over HTTPS);
 * 3. SSH-RSA  (Used for private repositories over SSH with a RSA Private Key).
 *
 * @author Felipe Bonezi
 * @since 20.10.29
 */
public class GitProvider extends AbstractProvider {

    /**
     * Contains the provider version.
     */
    private static String providerVersion;

    @Override
    public String getName() {
        return "Git";
    }

    @Override
    public String getVersion() {
        if (GitProvider.providerVersion == null) {
            synchronized (GitProvider.class) {
                final Properties properties = new Properties();
                final InputStream is = GitProvider.class.getClassLoader()
                    .getResourceAsStream("playrconf-git.properties");
                try {
                    properties.load(is);
                    GitProvider.providerVersion = properties.getProperty("playrconf.git.version", "unknown");
                    properties.clear();
                } catch (final IOException ignore) {
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return GitProvider.providerVersion;
    }

    @Override
    public String getConfigurationObjectName() {
        return "git";
    }

    @Override
    public void loadData(final Config config,
                         final Consumer<KeyValueCfgObject> kvObjConsumer,
                         final Consumer<FileCfgObject> fileObjConsumer) throws ConfigException, RemoteConfException {
        this.checkRequiredConfigFields(config);

        try {
            final String mode = config.getString("mode").trim();
            final String repositoryURI = config.getString("uri").trim();
            final String filepath = config.getString("filepath").trim();

            final Repository repository = this.cloneRepository(config, repositoryURI, mode);
            final ObjectId head = repository.resolve(Constants.HEAD);
            final RevCommit lastCommit = repository.parseCommit(head);

            final String conf = readFile(repository, lastCommit, filepath);
            final ConfigParseOptions options = ConfigParseOptions
                .defaults()
                .setOriginDescription("play-rconf")
                .setAllowMissing(false);

            final Config remoteConfig = ConfigFactory.parseString(conf, options);
            remoteConfig.entrySet().forEach(entry -> {
                final String value = entry.getValue().render();
                if (isFile(value)) {
                    fileObjConsumer.accept(
                        new FileCfgObject(entry.getKey(), value)
                    );
                } else {
                    kvObjConsumer.accept(
                        new KeyValueCfgObject(entry.getKey(), value)
                    );
                }
            });
        } catch (final ConfigException ex2) {
            if (ex2.getCause() != null) {
                throw new ConfigException.BadPath(
                    config.getString("uri"),
                    ex2.getCause().getClass().getName(),
                    ex2.getCause()
                );
            } else {
                throw new ConfigException.ValidationFailed(
                    Collections.singletonList(
                        new ConfigException.ValidationProblem(
                            config.getString("uri"),
                            ex2.origin(),
                            ex2.getMessage()
                        )
                    )
                );
            }
        } catch (MalformedURLException | JGitInternalException | GitAPIException e) {
            throw new ConfigException.BadPath("uri", e.getMessage());
        } catch (IOException e) {
            throw new RemoteConfException(e.getMessage(), e);
        }
    }

    /**
     * Check if all required fields are set in the project config file.
     *
     * @param config Config file
     */
    private void checkRequiredConfigFields(final Config config) throws ConfigException {
        if (!config.hasPath("mode") || config.getString("mode").isEmpty())
            throw new ConfigException.Missing("mode");

        if (!config.hasPath("uri") || config.getString("uri").isEmpty())
            throw new ConfigException.Missing("uri");

        if (!config.hasPath("filepath") || config.getString("filepath").isEmpty())
            throw new ConfigException.Missing("filepath");

        final String mode = config.getString("mode").trim();
        final String repositoryURI = config.getString("uri").trim();

        if (Objects.equals(mode, "user")) {
            if (!repositoryURI.startsWith("http")) {
                throw new ConfigException.BadPath("mode", String.format("Invalid repository URI for %s mode.", mode));
            }

            if (!config.hasPath("user.login") || config.getString("user.login").isEmpty()) {
                throw new ConfigException.Missing("user.login");
            } else if (!config.hasPath("user.password") || config.getString("user.password").isEmpty()) {
                throw new ConfigException.Missing("user.password");
            }
        } else if (Objects.equals(mode, "ssh-rsa")) {
            if (!repositoryURI.startsWith("git@")) {
                throw new ConfigException.BadPath("mode", String.format("Invalid repository URI for %s mode.", mode));
            }

            if (!config.hasPath("ssh-rsa.private-key") || config.getString("ssh-rsa.private-key").isEmpty()) {
                throw new ConfigException.Missing("ssh-rsa.private-key");
            }
        }
    }

    /**
     * Clones main branch of Git repository.
     *
     * @param config        Config file
     * @param repositoryURI Repository URI using HTTPS or SSH
     * @param mode          Auth mode
     * @return Repository
     */
    private Repository cloneRepository(final Config config, final String repositoryURI, final String mode) throws GitAPIException, IOException {
        final String dirPath = String.format("play-rconf-git-%s", System.currentTimeMillis());
        final Path repoDirPath = Files.createTempDirectory(dirPath);
        final CloneCommand cloneCommand = Git.cloneRepository()
            .setURI(repositoryURI)
            .setDirectory(repoDirPath.toFile());

        switch (mode) {
            case "ssh-rsa":
                final SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {

                    @Override
                    protected JSch createDefaultJSch(final FS fs) throws JSchException {
                        // SSH configuration (Optional password)
                        final String privateKey = config.getString("ssh-rsa.private-key");
                        final String password = config.hasPath("ssh-rsa.password")
                            ? config.getString("ssh-rsa.password") : null;
                        final JSch jSch = super.createDefaultJSch(fs);
                        jSch.addIdentity(privateKey, password);
                        return jSch;
                    }

                };
                cloneCommand.setTransportConfigCallback(transport -> {
                    final SshTransport sshTransport = (SshTransport) transport;
                    sshTransport.setSshSessionFactory(sshSessionFactory);
                });
                break;

            case "user":
                final String username = config.getString("user.login");
                final String password = config.getString("user.password");
                cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
                break;

            default:
                // Public repository over HTTPS
                break;
        }

        return cloneCommand.call().getRepository();
    }

    /**
     * Read config file from repository.
     *
     * @param repository Repository ref
     * @param commit     Last commit ref from the main branch
     * @param filepath   Path to retrieve the config content
     * @return Config content as String
     */
    private String readFile(final Repository repository, final RevCommit commit, final String filepath) throws IOException {
        final TreeWalk walk = TreeWalk.forPath(repository, filepath, commit.getTree());
        if (walk == null) {
            throw new IllegalArgumentException(String.format("Filepath (%s) not found.", filepath));
        }

        final byte[] bytes = repository.open(walk.getObjectId(0)).getBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

}
