bin/sric ./library/net/module.scm -fmake
bin/sric ./library/net/module.scm -fmake -debug

fan fmake ./output/sricNet.fmake -c emcc -f
