
USERX_ROOT = ../../..
include $(USERX_ROOT)/arch/components.mk

NGINX_URI=http://nginx.org/download/
NGINX_VER=1.0.14
NGINX_SRC=nginx-$(NGINX_VER).tar.gz
NGINX_DESTDIR=../../../../built/nginx/scratch/web_agents/nginx_agent/

all: install
	echo OS_ARCH: $(OS_ARCH)
	echo BUILD_TYPE: $(BUILD_TYPE)
	echo BUILD_DEBUG: $(BUILD_DEBUG)

$(NGINX_SRC):
	wget $(NGINX_URI)$(NGINX_SRC)

nginx-build: $(NGINX_SRC)
	tar xzf $(NGINX_SRC)
	mv nginx-$(NGINX_VER) nginx-build

nginx-build/Makefile: nginx-build
	cd nginx-build && \
	./configure --prefix=./ \
		--add-module=../ \
		--with-http_ssl_module

build: nginx-build/Makefile
	make -C nginx-build

install: build
	make -C nginx-build install DESTDIR=$(NGINX_DESTDIR)

clean:
	rm -rf nginx-build
	rm -rf $(NGINX_DEST)
