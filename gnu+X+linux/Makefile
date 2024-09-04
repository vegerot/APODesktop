PREFIX ?= /usr/local
install: .install-stamp
.install-stamp: ./apodesktop ./systemd/apodesktop.service ./systemd/apodesktop.timer
	install -v ./apodesktop $(PREFIX)/bin/apodesktop
	mkdir -p ~/.config/systemd/user
	install -v ./systemd/* ~/.config/systemd/user/
	systemctl --user enable apodesktop.timer 
	systemctl --user start apodesktop.timer
	touch .install-stamp
	$(PREFIX)/bin/apodesktop

uninstall:
	rm $(PREFIX)/bin/apodesktop || true
	rm ~/.config/systemd/user/apodesktop.service || true
	rm ~/.config/systemd/user/apodesktop.timer || true
	systemctl --user stop apodesktop.timer || true
	systemctl --user disable apodesktop.timer || true
	rm .install-stamp || true
