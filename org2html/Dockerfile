FROM ubuntu:20.04

# Set timezone, needed for some apt packages.
RUN apt-get -y update; apt-get -y upgrade
RUN apt-get -y install tzdata
RUN ln -fs /usr/share/zoneinfo/Universal /etc/localtime
RUN dpkg-reconfigure --frontend noninteractive tzdata

# Remaining apt packages
RUN apt-get -y install ca-certificates emacs-nox git python3

# Add .emacs.d lisp directory
RUN mkdir -p /root/.emacs.d/lisp/
COPY init.el /root/.emacs.d/init.el

# SSL
RUN update-ca-certificates

# Run w/ init.el
RUN emacs --script /root/.emacs.d/init.el

# Get emacs-htmlize
RUN git clone https://github.com/hniksic/emacs-htmlize.git /root/.emacs.d/lisp/emacs-htmlize

# Test that Emacs can export a simple org file to html
COPY test.el /root/test.el
COPY test.org /root/test.org
RUN emacs --script /root/test.el

# Add main python script
COPY org2html.py .

ENTRYPOINT ["python3"]
