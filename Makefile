.PHONY: build install
build:
	$(MAKE) -C macOS
install: build
	$(MAKE) -C macOS install
