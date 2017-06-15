Hamcrest matcher generator for Protocol Buffers
--

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