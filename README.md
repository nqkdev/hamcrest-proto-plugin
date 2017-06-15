Hamcrest matcher generator for Protocol Buffers
--
[![Build Status](https://travis-ci.org/nqkdev/hamcrest-proto-plugin.svg?branch=master)](https://travis-ci.org/nqkdev/hamcrest-proto-plugin)

Usage
--

```xml
<plugin>
    <groupId>me.nqkdev.plugins</groupId>
    <artifactId>hamcrest-proto-plugin</artifactId>
    <version>1.0-SNAPSHOT</version>
    <executions>
        <execution>
            <id>generate-hamcrest-matchers</id>
            <goals>
                <goal>compile</goal>
            </goals>
            <configuration>
                <target>${protobuf.output.directory}</target>
            </configuration>
        </execution>
    </executions>
</plugin>
```