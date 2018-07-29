{ stdenv, gradle_2_5, jre, postgresql, redis, perl, writeText, writeShellScriptBin }:

let

  name = "i++";

  src = ./.;

  deps = stdenv.mkDerivation {
    name = "${name}-deps";
    inherit src;
    buildInputs = [ gradle_2_5 redis perl ];
    buildPhase = ''
      export GRADLE_USER_HOME=$(mktemp -d)
      gradle --no-daemon build
    '';

    installPhase = ''
      find $GRADLE_USER_HOME/caches/modules-2 -type f -regex '.*\.\(jar\|pom\)' \
        | perl -pe 's#(.*/([^/]+)/([^/]+)/([^/]+)/[0-9a-f]{30,40}/([^/\s]+))$# ($x = $2) =~ tr|\.|/|; "install -Dm444 $1 \$out/$x/$3/$4/$5" #e' \
        | sh
    '';
    outputHashAlgo = "sha256";
    outputHashMode = "recursive";
    outputHash = "169li6phhx19kgr0cx29f070hhqmddwk6zqhhcns71fg6w1y4wsa";
  };

  gradleInit = writeText "init.gradle" ''
    logger.lifecycle 'Replacing Maven repositories with ${deps}...'

    gradle.projectsLoaded {
      rootProject.allprojects {
        buildscript {
          repositories {
            clear()
            maven { url '${deps}' }
          }
        }
        repositories {
          clear()
          maven { url '${deps}' }
        }
      }
    }
  '';

  installScript = x: ''
    install -Dm755 ./${x}/build/install/${x}/bin/${x} \
      $out/bin/${x}

    substituteInPlace $out/bin/${x} --replace 'DEFAULT_JVM_OPTS=""' 'export JAVA_HOME="${jre}"'

    cp ./${x}/build/install/${x}/lib/*.jar \
      $out/lib
  '';

in stdenv.mkDerivation {
  inherit name;
  inherit src;

  buildInputs = [ gradle_2_5 redis jre ];

  buildPhase = ''
    export GRADLE_USER_HOME=$(mktemp -d)
    gradle --offline --no-daemon --info --init-script ${gradleInit} build
  '';

  installPhase = ''
    mkdir -p $out/lib

    ${installScript "Processor"}
    ${installScript "Publisher"}
    ${installScript "Replayer"}
    ${installScript "Reader"}
  '';
}
