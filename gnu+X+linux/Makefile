PREFIX ?= /usr/local
install:
	install ./apodesktop $(PREFIX)/bin/apodesktop
	install ./systemd/* ~/.config/systemd/user/
	systemctl --user enable apodesktop.timer 
	systemctl --user start apodesktop.timer
