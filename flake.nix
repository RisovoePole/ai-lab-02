{
  description = "Java development environment with jdtls and Gradle";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };
      in
      {
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            jdk21
            jdt-language-server
            gradle
            just
          ];

          shellHook = ''
            export JAVA_HOME=${pkgs.jdk21}/lib/openjdk            export PATH="${pkgs.jdt-language-server}/bin:$PATH"
            echo "Java dev shell loaded: $(java -version 2>&1 | head -n 1)"
          '';
        };
      }
    );
}
