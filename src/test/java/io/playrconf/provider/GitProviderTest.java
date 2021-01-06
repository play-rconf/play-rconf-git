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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import io.playrconf.sdk.FileCfgObject;
import io.playrconf.sdk.Provider;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * GitProviderTest.
 * <p>
 * Default environment variables:
 * 1. REMOTECONF_GIT_BRANCH
 * 2. REMOTECONF_GIT_FILEPATH
 * </p>
 * <p>
 * Required environment variables:
 * 1. For none mode:
 * a) REMOTECONF_GIT_URI
 * 2. For user mode:
 * a) REMOTECONF_GIT_USER_URI
 * b) REMOTECONF_GIT_USER_LOGIN
 * c) REMOTECONF_GIT_USER_PASSWORD
 * </p>
 *
 * @author Felipe Boenzi
 * @since 20.10.29
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GitProviderTest {

    /**
     * Initial configuration for public repository.
     */
    private static final Config INITIAL_CONFIGURATION_FOR_NONE_MODE = ConfigFactory.parseString(
        "application.hello = \"Bad value\"\n"
            + "git.uri = \"" + System.getenv("REMOTECONF_GIT_URI") + "\"\n"
            + "git.filepath = \"" + System.getenv("REMOTECONF_GIT_FILEPATH") + "\"\n"
            + "git.mode = \"none\"\n"
    );

    /**
     * Initial configuration for private repository with login and password authentication.
     */
    private static final Config INITIAL_CONFIGURATION_FOR_USER_MODE = ConfigFactory.parseString(
        "application.hello = \"Bad value\"\n"
            + "git.uri = \"" + System.getenv("REMOTECONF_GIT_USER_URI") + "\"\n"
            + "git.filepath = \"" + System.getenv("REMOTECONF_GIT_FILEPATH") + "\"\n"
            + "git.mode = \"user\"\n"
            + "git.user.login = \"" + System.getenv("REMOTECONF_GIT_USER_LOGIN") + "\"\n"
            + "git.user.password = \"" + System.getenv("REMOTECONF_GIT_USER_PASSWORD") + "\"\n"
    );

    /**
     * Initial configuration. Git repository URI is empty.
     */
    private static final Config INITIAL_CONFIGURATION_ERROR_MISSING_URI = ConfigFactory.parseString(
        "application.hello = \"Bad value\"\n"
            + "git.uri = \"\"\n"
            + "git.filepath = \"" + System.getenv("REMOTECONF_GIT_FILEPATH") + "\"\n"
            + "git.mode = \"none\"\n"
    );

    /**
     * Initial configuration. Git repository URI is empty.
     */
    private static final Config INITIAL_CONFIGURATION_ERROR_MISSING_FILEPATH = ConfigFactory.parseString(
        "application.hello = \"Bad value\"\n"
            + "git.uri = \"" + System.getenv("REMOTECONF_GIT_URI") + "\"\n"
            + "git.filepath = \"\"\n"
            + "git.mode = \"none\"\n"
    );

    /**
     * Initial configuration. Git repository URI is empty.
     */
    private static final Config INITIAL_CONFIGURATION_ERROR_MISSING_MODE = ConfigFactory.parseString(
        "application.hello = \"Bad value\"\n"
            + "git.uri = \"" + System.getenv("REMOTECONF_GIT_URI") + "\"\n"
            + "git.filepath = \"" + System.getenv("REMOTECONF_GIT_FILEPATH") + "\"\n"
            + "git.mode = \"\"\n"
    );

    /**
     * Initial configuration. Git repository URI is empty.
     */
    private static final Config INITIAL_CONFIGURATION_ERROR_MISSING_LOGIN_FOR_USER_MODE = ConfigFactory.parseString(
        "application.hello = \"Bad value\"\n"
            + "git.uri = \"" + System.getenv("REMOTECONF_GIT_USER_URI") + "\"\n"
            + "git.filepath = \"" + System.getenv("REMOTECONF_GIT_FILEPATH") + "\"\n"
            + "git.mode = \"user\"\n"
            + "git.user.login = \"\"\n"
            + "git.user.password = \"" + System.getenv("REMOTECONF_GIT_USER_PASSWORD") + "\"\n"
    );

    /**
     * Initial configuration. Git repository URI is empty.
     */
    private static final Config INITIAL_CONFIGURATION_ERROR_MISSING_PASSWORD_FOR_USER_MODE = ConfigFactory.parseString(
        "application.hello = \"Bad value\"\n"
            + "git.uri = \"" + System.getenv("REMOTECONF_GIT_USER_URI") + "\"\n"
            + "git.filepath = \"" + System.getenv("REMOTECONF_GIT_FILEPATH") + "\"\n"
            + "git.mode = \"user\"\n"
            + "git.user.login = \"" + System.getenv("REMOTECONF_GIT_USER_LOGIN") + "\"\n"
            + "git.user.password = \"\"\n"
    );

    /**
     * Initial configuration. Git repository URI is empty.
     */
    private static final Config INITIAL_CONFIGURATION_ERROR_MISSING_PRIVATE_KEY_FOR_SSH_MODE = ConfigFactory.parseString(
        "application.hello = \"Bad value\"\n"
            + "git.uri = \"" + System.getenv("REMOTECONF_GIT_SSH_URI") + "\"\n"
            + "git.filepath = \"" + System.getenv("REMOTECONF_GIT_FILEPATH") + "\"\n"
            + "git.mode = \"ssh-rsa\"\n"
            + "git.ssh-rsa.private-key = \"\"\n"
    );

    /**
     * Initial configuration. Git repository URI is empty.
     */
    private static final Config INITIAL_CONFIGURATION_ERROR_URI_BAD_VALUE_FOR_NONE_MODE = ConfigFactory.parseString(
        "application.hello = \"Bad value\"\n"
            + "git.uri = \"git@github.com/play-rconf/play-rconf-git.git\"\n"
            + "git.filepath = \"" + System.getenv("REMOTECONF_GIT_FILEPATH") + "\"\n"
            + "git.mode = \"none\"\n"
    );

    /**
     * Initial configuration. Git repository URI is empty.
     */
    private static final Config INITIAL_CONFIGURATION_ERROR_URI_BAD_VALUE_FOR_USER_MODE = ConfigFactory.parseString(
        "application.hello = \"Bad value\"\n"
            + "git.uri = \"git@github.com/play-rconf/play-rconf-git.git\"\n"
            + "git.filepath = \"" + System.getenv("REMOTECONF_GIT_FILEPATH") + "\"\n"
            + "git.mode = \"user\"\n"
    );

    /**
     * Initial configuration. Git repository URI is empty.
     */
    private static final Config INITIAL_CONFIGURATION_ERROR_URI_BAD_VALUE_FOR_SSH_RSA_MODE = ConfigFactory.parseString(
        "application.hello = \"Bad value\"\n"
            + "git.uri = \"https://github.com/play-rconf/play-rconf-git\"\n"
            + "git.filepath = \"" + System.getenv("REMOTECONF_GIT_FILEPATH") + "\"\n"
            + "git.mode = \"ssh-rsa\"\n"
    );

    /**
     * Initial configuration. Git repository URI is empty.
     */
    private static final Config INITIAL_CONFIGURATION_ERROR_UNKNOWN_GIT_URI = ConfigFactory.parseString(
        "application.hello = \"Bad value\"\n"
            + "git.uri = \"https://doma1n-do3s-not-3x15t5-2832893729387.com\"\n"
            + "git.filepath = \"" + System.getenv("REMOTECONF_GIT_FILEPATH") + "\"\n"
            + "git.mode = \"none\"\n"
    );

    @Test
    public void gitTest_001() {
        final StringBuilder stringBuilder = new StringBuilder(512);
        final Provider provider = new GitProvider();
        provider.loadData(
            INITIAL_CONFIGURATION_FOR_NONE_MODE.getConfig(provider.getConfigurationObjectName()),
            keyValueCfgObject -> keyValueCfgObject.apply(stringBuilder),
            FileCfgObject::apply
        );
        final Config remoteConfig = ConfigFactory
            .parseString(stringBuilder.toString())
            .withFallback(INITIAL_CONFIGURATION_FOR_NONE_MODE);

        // Test version
        final Properties properties = new Properties();
        try (InputStream is = GitProvider.class.getClassLoader()
            .getResourceAsStream("playrconf-git.properties")) {
            properties.load(is);
            Assert.assertEquals(
                provider.getVersion(),
                properties.getProperty("playrconf.git.version", "unknown")
            );
            properties.clear();
        } catch (final IOException ignore) {
        }

        // Standard values
        Assert.assertEquals(5, remoteConfig.getInt("application.five"));
        Assert.assertEquals("world", remoteConfig.getString("application.hello"));
        Assert.assertTrue(remoteConfig.getBoolean("application.is-enabled"));
    }

    @Test
    public void gitTest_002() {
        final StringBuilder stringBuilder = new StringBuilder(512);
        final Provider provider = new GitProvider();
        provider.loadData(
            INITIAL_CONFIGURATION_FOR_USER_MODE.getConfig(provider.getConfigurationObjectName()),
            keyValueCfgObject -> keyValueCfgObject.apply(stringBuilder),
            FileCfgObject::apply
        );
        final Config remoteConfig = ConfigFactory
            .parseString(stringBuilder.toString())
            .withFallback(INITIAL_CONFIGURATION_FOR_USER_MODE);

        // Standard values
        Assert.assertEquals(5, remoteConfig.getInt("application.five"));
        Assert.assertEquals("world", remoteConfig.getString("application.hello"));
        Assert.assertTrue(remoteConfig.getBoolean("application.is-enabled"));
    }

    @Test(expected = ConfigException.Missing.class)
    public void gitTest_005() {
        loadConfigWithError(INITIAL_CONFIGURATION_ERROR_MISSING_URI);
    }

    @Test(expected = ConfigException.Missing.class)
    public void gitTest_007() {
        loadConfigWithError(INITIAL_CONFIGURATION_ERROR_MISSING_FILEPATH);
    }

    @Test(expected = ConfigException.Missing.class)
    public void gitTest_008() {
        loadConfigWithError(INITIAL_CONFIGURATION_ERROR_MISSING_MODE);
    }

    @Test(expected = ConfigException.Missing.class)
    public void gitTest_009() {
        loadConfigWithError(INITIAL_CONFIGURATION_ERROR_MISSING_LOGIN_FOR_USER_MODE);
    }

    @Test(expected = ConfigException.Missing.class)
    public void gitTest_010() {
        loadConfigWithError(INITIAL_CONFIGURATION_ERROR_MISSING_PASSWORD_FOR_USER_MODE);
    }

    @Test(expected = ConfigException.Missing.class)
    public void gitTest_011() {
        loadConfigWithError(INITIAL_CONFIGURATION_ERROR_MISSING_PRIVATE_KEY_FOR_SSH_MODE);
    }

    @Test(expected = ConfigException.BadPath.class)
    public void gitTest_012() {
        loadConfigWithError(INITIAL_CONFIGURATION_ERROR_URI_BAD_VALUE_FOR_NONE_MODE);
    }

    @Test(expected = ConfigException.BadPath.class)
    public void gitTest_013() {
        loadConfigWithError(INITIAL_CONFIGURATION_ERROR_URI_BAD_VALUE_FOR_USER_MODE);
    }

    @Test(expected = ConfigException.BadPath.class)
    public void gitTest_014() {
        loadConfigWithError(INITIAL_CONFIGURATION_ERROR_URI_BAD_VALUE_FOR_SSH_RSA_MODE);
    }

    @Test(expected = ConfigException.BadPath.class)
    public void gitTest_015() {
        loadConfigWithError(INITIAL_CONFIGURATION_ERROR_UNKNOWN_GIT_URI);
    }

    private void loadConfigWithError(Config initialConfigurationErrorEmptyFilepath) throws ConfigException {
        final StringBuilder stringBuilder = new StringBuilder(512);
        final Provider provider = new GitProvider();
        provider.loadData(
            initialConfigurationErrorEmptyFilepath.getConfig(provider.getConfigurationObjectName()),
            keyValueCfgObject -> keyValueCfgObject.apply(stringBuilder),
            FileCfgObject::apply
        );
        ConfigFactory
            .parseString(stringBuilder.toString())
            .withFallback(initialConfigurationErrorEmptyFilepath);
    }

}
