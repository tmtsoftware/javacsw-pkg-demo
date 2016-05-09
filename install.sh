#!/bin/sh
#
# Creates a single install directory from all the csw stage directories.

dir=../install

test -d $dir || mkdir -p $dir/bin $dir/lib $dir/conf
sbt publish-local stage

for i in bin lib ; do
    for j in */target/universal/stage/$i/* ; do
        cp -f $j $dir/$i
    done
done

rm -f $dir/bin/*.log.* $dir/bin/*.bat
(cd hardware/src/main/c; make -f Makefile.`uname`; cp mtserver2 ../../../../../install/bin/)

chmod ugo+x scripts/*
cp scripts/* $dir/bin/



