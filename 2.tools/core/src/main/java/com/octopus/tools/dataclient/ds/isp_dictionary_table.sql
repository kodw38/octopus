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
/*Table structure for table `isp_dictionary_table` */

CREATE TABLE `isp_dictionary_table` (
  `TABLE_ID` int(6) DEFAULT NULL,
  `TABLE_NAME` varchar(500) DEFAULT NULL,
  `FIELD_CODE` varchar(50) DEFAULT NULL,
  `STATE` int(11) DEFAULT NULL,
  `USED_TYPES` varchar(12) DEFAULT NULL,
  `NOT_NULL` int(1) DEFAULT NULL,
  `TABLE_NUM` int(6) DEFAULT NULL,
  `REMARK` varchar(1024) DEFAULT NULL,
  `IS_CACHE` int(1) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=gbk;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
