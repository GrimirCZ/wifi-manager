package cz.grimir.wifimanager.captive.persistence

import cz.grimir.wifimanager.captive.persistence.entity.CaptiveDeviceEntity
import org.springframework.data.jpa.repository.JpaRepository

interface CaptiveDeviceJpaRepository : JpaRepository<CaptiveDeviceEntity, String>
