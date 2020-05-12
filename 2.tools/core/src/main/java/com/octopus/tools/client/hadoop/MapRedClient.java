package com.octopus.tools.client.hadoop;

/**
 * MapReduce主要的价值是依赖于hdfs的存储， hdfs上的文件按块大小分多个map并行执行，可以reduce分组并发执行的处理数据。
 * 该类功能，是让开发不需要写code，而是配置，有该类提交mapreduce到yarn中执行，输入，输出都是hdfs上的文件。
 * map是对分块的数据一行一行的处理，输出一个key-value的数据
 * reduct是对key，list<value>的数据处理，最后输出到hdfs
 * map中的一行数据的处理用，octopus表示？
 * reduce用octopus的字符串表示？
 * Created by admin on 2020/3/8.
 */
public class MapRedClient {
}
