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
/*Table structure for table `isp_dictionary_field` */

CREATE TABLE `isp_dictionary_field` (
  `FIELD_ID` int(6) DEFAULT NULL,
  `FIELD_NAME` varchar(100) DEFAULT NULL,
  `FIELD_CODE` varchar(50) NOT NULL,
  `FIELD_TYPE` varchar(2) DEFAULT NULL COMMENT 'L:Long,I:Int,D:Double,S:String,T:Date,B:Boolean',
  `STATE` int(11) DEFAULT NULL,
  `REMARK` varchar(256) DEFAULT NULL,
  `FIELD_LEN` int(5) DEFAULT NULL,
  `FIELD_NUM` varchar(5) DEFAULT NULL,
  PRIMARY KEY (`FIELD_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=gbk;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
