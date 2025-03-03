FROM adoptopenjdk/openjdk8
MAINTAINER Kasper Luckow <kasper.luckow@sv.cmu.edu>

#############################################################################
# Setup base image 
#############################################################################
RUN \
  apt-get update -y && \
  apt-get install software-properties-common -y && \
  apt-get update -y && \
  apt-get install -y ant \
                maven \
                git \
                junit \
                build-essential \
                python \
                antlr3 \
                wget \
                unzip \
                mercurial \
                vim && \
  rm -rf /var/lib/apt/lists/* 

#############################################################################
# Environment 
#############################################################################

# set java env
# ENV JAVA_HOME /usr/lib/jvm/java-8-oracle
ENV JUNIT_HOME /usr/share/java

# Make dir for distribution
WORKDIR /
RUN mkdir tools
ENV TOOLS_ROOT /tools


#############################################################################
# Install and configure jpf-related tools 
#############################################################################

# Set up jpf conf initially
RUN mkdir /root/.jpf
RUN echo "jpf-core = ${TOOLS_ROOT}/jpf-core" >> /root/.jpf/site.properties
RUN echo "jpf-symbc = ${TOOLS_ROOT}/jpf-symbc" >> /root/.jpf/site.properties
RUN echo "starlib = ${TOOLS_ROOT}/starlib" >> /root/.jpf/site.properties
RUN echo "jpf-star = ${TOOLS_ROOT}/jpf-star" >> /root/.jpf/site.properties
RUN echo "jpf-costar = ${TOOLS_ROOT}/jpf-costar" >> /root/.jpf/site.properties

# Set extensions var
RUN echo "extensions=\${jpf-core},\${jpf-symbc},\${starlib},\${jpf-star},\${jpf-costar}" >> /root/.jpf/site.properties

# Install jpf-core
WORKDIR ${TOOLS_ROOT}
RUN git clone https://github.com/star-finder/jpf-core

WORKDIR ${TOOLS_ROOT}/jpf-core
RUN ant

# Install jpf-symbc
WORKDIR ${TOOLS_ROOT}
RUN git clone https://github.com/star-finder/jpf-symbc

WORKDIR ${TOOLS_ROOT}/jpf-symbc
RUN ant
# Install s2sat solver
WORKDIR ${TOOLS_ROOT}
RUN git clone https://github.com/star-finder/s2sat
# The s2sat solver uses the absolute path
RUN cp -a s2sat/. /usr/local/bin/

WORKDIR ${TOOLS_ROOT}
RUN git clone https://github.com/star-finder/starlib

WORKDIR ${TOOLS_ROOT}/starlib
RUN ant

WORKDIR ${TOOLS_ROOT}
RUN git clone https://github.com/star-finder/jpf-star

# Finally, get jpf-star
WORKDIR ${TOOLS_ROOT}
RUN git clone https://github.com/benjamin-hejl/jpf-costar.git

WORKDIR ${TOOLS_ROOT}/jpf-star
RUN ant

WORKDIR ${TOOLS_ROOT}/jpf-costar
RUN ant

# Let's go!
WORKDIR ${TOOLS_ROOT}/jpf-star
