
USERX_ROOT = ../../..
include $(USERX_ROOT)/arch/components.mk

NGINX_URI=http://nginx.org/download/
NGINX_VER=1.0.14
NGINX_SRC=nginx-$(NGINX_VER).tar.gz
NGINX_DEST=../nginx_agent/

all: install
	echo OS_ARCH: $(OS_ARCH)
	echo BUILD_TYPE: $(BUILD_TYPE)
	echo BUILD_DEBUG: $(BUILD_DEBUG)

$(NGINX_SRC):
	wget $(NGINX_URI)$(NGINX_SRC)

nginx-$(NGINX_VER): $(NGINX_SRC)
	tar xzf $(NGINX_SRC)

nginx-$(NGINX_VER)/Makefile: nginx-$(NGINX_VER)
	tar xzf $(NGINX_SRC)
	cd nginx-$(NGINX_VER) && \
	./configure --prefix=./ \
		--sbin-path=sbin/nginx-agent \
		--add-module=../ \
		--with-http_ssl_module

build: nginx-$(NGINX_VER)/Makefile
	make -C nginx-$(NGINX_VER)

install: build
	make -C nginx-$(NGINX_VER) install DESTDIR=$(NGINX_DEST)

clean:
	rm -rf nginx-$(NGINX_VER)
	rm -rf $(NGINX_DEST)
