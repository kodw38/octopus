/*
SQLyog Ultimate v8.4 
MySQL - 5.6.10-log : Database - mysql
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*Table structure for table `isp_dictionary_table_router` */

CREATE TABLE `isp_dictionary_table_router` (
  `ROUTER_ID` int(6) DEFAULT NULL,
  `DATA_SOURCE` varchar(50) DEFAULT NULL,
  `TABLE_NAME` varchar(50) DEFAULT NULL,
  `SPLIT_EXPRESS` varchar(1200) DEFAULT NULL COMMENT '根据数据拼后缀',
  `ROUTE_EXPRESS` varchar(1200) DEFAULT NULL COMMENT '根据数据判断是否存储在该表',
  `STATE` int(1) DEFAULT NULL,
  `SPLIT_RANGE` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=gbk;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
