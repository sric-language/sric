set -e

fmake ../jsonc/libjsonc.props -c emcc  -f

fmake ./output/sric.fmake -c emcc  -f
fmake ./output/serial.fmake -c emcc  -f

