-- Add bin column to banks table in retail-platform-master
-- Run this against retail-platform-master database

ALTER TABLE `banks`
    ADD COLUMN `bin` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL
        COMMENT 'VietQR BIN code for QR generation and logo display'
        AFTER `code`;

UPDATE `banks` SET bin='970436' WHERE code='VCB';
UPDATE `banks` SET bin='970415' WHERE code='CTG';
UPDATE `banks` SET bin='970418' WHERE code='BID';
UPDATE `banks` SET bin='970405' WHERE code='AGR';
UPDATE `banks` SET bin='970422' WHERE code='MBB';
UPDATE `banks` SET bin='970407' WHERE code='TCB';
UPDATE `banks` SET bin='970432' WHERE code='VPB';
UPDATE `banks` SET bin='970416' WHERE code='ACB';
UPDATE `banks` SET bin='970403' WHERE code='STB';
UPDATE `banks` SET bin='970423' WHERE code='TPB';
UPDATE `banks` SET bin='970437' WHERE code='HDB';
UPDATE `banks` SET bin='970441' WHERE code='VIB';
UPDATE `banks` SET bin='970443' WHERE code='SHB';
UPDATE `banks` SET bin='970431' WHERE code='EIB';
UPDATE `banks` SET bin='970449' WHERE code='LPB';
UPDATE `banks` SET bin='970426' WHERE code='MSB';
UPDATE `banks` SET bin='970448' WHERE code='OCB';
UPDATE `banks` SET bin='970440' WHERE code='SSB';
UPDATE `banks` SET bin='970425' WHERE code='ABB';
UPDATE `banks` SET bin='970409' WHERE code='BAB';
UPDATE `banks` SET bin='970454' WHERE code='BVB';
UPDATE `banks` SET bin='970462' WHERE code='KLB';
UPDATE `banks` SET bin='970428' WHERE code='NAB';
UPDATE `banks` SET bin='970419' WHERE code='NCB';
UPDATE `banks` SET bin='970430' WHERE code='PGB';
UPDATE `banks` SET bin='970452' WHERE code='PVCB';
UPDATE `banks` SET bin='970400' WHERE code='SGB';
UPDATE `banks` SET bin='970433' WHERE code='VBB';
UPDATE `banks` SET bin='970438' WHERE code='BVK';
UPDATE `banks` SET bin='970408' WHERE code='GPB';
UPDATE `banks` SET bin='970414' WHERE code='OJB';
UPDATE `banks` SET bin='970406' WHERE code='DAB';
UPDATE `banks` SET bin='970444' WHERE code='CBB';
UPDATE `banks` SET bin='546034' WHERE code='CAKE';
UPDATE `banks` SET bin='963388' WHERE code='TIMO';
UPDATE `banks` SET bin='458761' WHERE code='HSBC';
UPDATE `banks` SET bin='970410' WHERE code='SC';
UPDATE `banks` SET bin='970424' WHERE code='SHIN';
UPDATE `banks` SET bin='970457' WHERE code='WOORI';
UPDATE `banks` SET bin='970458' WHERE code='UOB';
UPDATE `banks` SET bin='970442' WHERE code='HLB';
UPDATE `banks` SET bin='970439' WHERE code='PBB';
UPDATE `banks` SET bin='970455' WHERE code='IBK';
UPDATE `banks` SET bin='668888' WHERE code='KEXIM';
