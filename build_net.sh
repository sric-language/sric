bin/sric ./library/net/module.scm -fmake
bin/sric ./library/net/module.scm -fmake -debug

fmake ./output/sricNet.fmake -c emcc -f
