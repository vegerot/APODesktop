.PHONY: build install
build:
	bash -c "if [[ '$${OSTYPE}' = 'darwin'* ]]; then $(MAKE) --directory=macOS ; else $(MAKE) --directory='gnu+X+linux'; fi"

install: build
	bash -c "if [[ '$${OSTYPE}' = 'darwin'* ]]; then $(MAKE) --directory=macOS install; else $(MAKE) --directory='gnu+X+linux' install; fi"
