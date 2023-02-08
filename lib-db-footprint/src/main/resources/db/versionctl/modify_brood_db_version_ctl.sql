ALTER TABLE `brood_db_version_ctl` ADD `extend_version` INT NOT NULL DEFAULT 0 AFTER `patch_version`;

ALTER TABLE `brood_db_version_ctl` DROP KEY `brood_db_version_ctl_unique01`;
ALTER TABLE `brood_db_version_ctl` ADD CONSTRAINT `brood_db_version_ctl_unique01` UNIQUE KEY (`business_space`,`major_version`,`minor_version`,`patch_version`,`extend_version`);
