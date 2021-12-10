set -eou pipefail
docker build . -t davidfrickert/photon:latest -t davidfrickert/photon:11
docker push davidfrickert/photon:latest
docker push davidfrickert/photon:11