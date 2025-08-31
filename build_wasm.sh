set -e

fan fmake ../jsonc/libjsonc.props -c emcc  -f
fan fmake ./output/sric.fmake -c emcc  -f
fan fmake ./output/cstd.fmake -c emcc  -f
fan fmake ./output/serial.fmake -c emcc  -f

