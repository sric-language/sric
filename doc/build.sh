cd doc-zh
mdbook build
cp -r book/* ../../../sric-language.github.io/doc_zh/
cd ..

cd doc-en
mdbook build
cp -r book/* ../../../sric-language.github.io/doc_en/
cd ..


cp ../output/sric.html ../../sric-language.github.io/apidoc/
cp ../output/cstd.html ../../sric-language.github.io/apidoc/
cp ../output/jsonc.html ../../sric-language.github.io/apidoc/
cp ../output/serial.html ../../sric-language.github.io/apidoc/