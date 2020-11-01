# Play Remote Configuration - GIT


Retrieves configuration hosted on a GIT repository
*****

## About this project
In production, it is not always easy to manage the configuration files of a
Play Framework application, especially when it running on multiple servers.
The purpose of this project is to provide a simple way to use a remote
configuration with a Play Framework application.



## How to use

To enable this provider, just add the classpath `"io.playrconf.provider.GitProvider"`
and the following configuration:

```hocon
remote-configuration {

  ## Provider - GIT
  # ~~~~~
  # Retrieves configuration hosted on a GIT repository
  git {

  }
}
```
You can use `io.playrconf.GitProvier` with three auth modes:
* None (For public repositories);
* User (For private repositories over login/password);
* SSH-RSA (For private repositories over SSH-RSA read only file).

If you use `ssh-rsa` mode, then you must provide the file path on `ssh-rsa.privateKey` config. If your private key has 
a password, then you must provide `ssh-rsa.password`.


## License
This project is released under terms of the [MIT license](https://raw.githubusercontent.com/play-rconf/play-rconf-git/master/LICENSE).
