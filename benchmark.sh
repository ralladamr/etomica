#!/usr/bin/env bash

[[ "${PWD}" == "$(git rev-parse --show-toplevel)" ]] || {
    echo "You must run this script in the root of the repository";
    exit 1
}

TIMESTAMP=$(date +%FT%T)
COMMIT=$(git rev-parse --short HEAD)

OUTDIR="${PWD}/benchmarks/build/tmp/results"

while getopts "o:" opt; do
    case "${opt}" in
        o)
            OUTDIR=${OPTARG}
            ;;
    esac
done
shift $((OPTIND-1))

[[ ! -d ${OUTDIR} ]] && mkdir -p ${OUTDIR}

./gradlew --no-daemon :benchmarks:shadowJar

java -jar ./benchmarks/build/libs/benchmarks-all.jar BenchSim -rf JSON -rff "${OUTDIR}/${TIMESTAMP}_${COMMIT}.json"
