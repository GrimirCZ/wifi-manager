package cz.grimir.wifimanager.admin.web.mvc

import cz.grimir.wifimanager.admin.application.commands.DeleteAllowedMacCommand
import cz.grimir.wifimanager.admin.application.commands.UpsertAllowedMacCommand
import cz.grimir.wifimanager.admin.application.usecases.commands.AdminDeleteAllowedMacUsecase
import cz.grimir.wifimanager.admin.application.usecases.commands.AdminUpsertAllowedMacUsecase
import cz.grimir.wifimanager.admin.application.usecases.queries.AdminListAllowedMacsUsecase
import cz.grimir.wifimanager.admin.application.usecases.queries.AdminListNetworkClientsUsecase
import cz.grimir.wifimanager.admin.web.mvc.dto.AllowedMacRequestDto
import cz.grimir.wifimanager.admin.web.mvc.dto.AllowedMacViewDto
import cz.grimir.wifimanager.admin.web.mvc.dto.NetworkClientViewDto
import cz.grimir.wifimanager.shared.application.UserIdentitySnapshot
import cz.grimir.wifimanager.shared.application.network.NetworkClient
import cz.grimir.wifimanager.shared.core.UserRole
import cz.grimir.wifimanager.shared.security.mvc.CurrentUser
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.server.ResponseStatusException
import java.time.Duration

@Controller
class AdminAllowedMacController(
    private val upsertAllowedMacUsecase: AdminUpsertAllowedMacUsecase,
    private val deleteAllowedMacUsecase: AdminDeleteAllowedMacUsecase,
    private val listAllowedMacsUsecase: AdminListAllowedMacsUsecase,
    private val listNetworkClientsUsecase: AdminListNetworkClientsUsecase,
) {
    companion object {
        private val MAC_ADDRESS_REGEX = Regex("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}")
    }

    @GetMapping("/admin/allowed-mac")
    fun allowedMacsPage(
        @CurrentUser user: UserIdentitySnapshot,
        modelMap: ModelMap,
    ): String {
        ensureAllowedMacAccess(user)
        modelMap.addAttribute("canManageAllowedMacs", true)
        modelMap.addAttribute("allowedMacs", loadAllowedMacViews())
        modelMap.addAttribute("allowedMac", AllowedMacRequestDto())
        modelMap.addAttribute("networkClients", emptyList<NetworkClient>())
        return "admin/allowed-mac"
    }

    @GetMapping("/admin/_allowed-mac")
    fun allowedMacs(
        @CurrentUser user: UserIdentitySnapshot,
        modelMap: ModelMap,
    ): String {
        ensureAllowedMacAccess(user)
        modelMap.addAttribute("allowedMacs", loadAllowedMacViews())
        modelMap.addAttribute("allowedMac", AllowedMacRequestDto())
        return "admin/fragments/allowed-mac :: allowedMacList"
    }

    @GetMapping("/admin/_network-clients")
    fun networkClients(
        @CurrentUser user: UserIdentitySnapshot,
        modelMap: ModelMap,
    ): String {
        ensureAllowedMacAccess(user)
        modelMap.addAttribute("networkClients", loadNetworkClientViews())
        return "admin/fragments/network-clients :: networkClientList"
    }

    @PostMapping("/admin/allowed-mac")
    fun upsertAllowedMac(
        @CurrentUser user: UserIdentitySnapshot,
        @ModelAttribute request: AllowedMacRequestDto,
        modelMap: ModelMap,
    ): String {
        ensureAllowedMacAccess(user)
        val mac = MacAddressNormalizer.normalize(request.mac)
        val errorKey = validateMac(mac)
        if (errorKey != null) {
            modelMap.addAttribute("allowedMacErrorKey", errorKey)
        } else {
            val duration = request.validityMinutes?.takeIf { it > 0 }?.let { Duration.ofMinutes(it) }
            upsertAllowedMacUsecase.upsert(
                UpsertAllowedMacCommand(
                    macAddress = mac,
                    hostname = request.hostname?.takeIf { it.isNotBlank() },
                    note = request.note.trim(),
                    validityDuration = duration,
                    user = user,
                ),
            )
            modelMap.addAttribute("allowedMac", AllowedMacRequestDto())
        }

        if (!modelMap.containsKey("allowedMac")) {
            modelMap.addAttribute("allowedMac", request)
        }
        modelMap.addAttribute("allowedMacs", loadAllowedMacViews())
        return "admin/fragments/allowed-mac :: allowedMacList"
    }

    @DeleteMapping("/admin/allowed-mac/{mac}")
    fun deleteAllowedMac(
        @CurrentUser user: UserIdentitySnapshot,
        @PathVariable mac: String,
        modelMap: ModelMap,
    ): String {
        ensureAllowedMacAccess(user)
        val normalizedMac = MacAddressNormalizer.normalize(mac)
        deleteAllowedMacUsecase.delete(DeleteAllowedMacCommand(macAddress = normalizedMac, user = user))
        modelMap.addAttribute("allowedMacs", loadAllowedMacViews())
        modelMap.addAttribute("allowedMac", AllowedMacRequestDto())
        return "admin/fragments/allowed-mac :: allowedMacList"
    }

    private fun loadAllowedMacViews(): List<AllowedMacViewDto> =
        listAllowedMacsUsecase
            .list()
            .map {
                AllowedMacViewDto(
                    mac = it.mac,
                    hostname = it.hostname,
                    ownerDisplayName = it.ownerDisplayName,
                    ownerEmail = it.ownerEmail,
                    note = it.note,
                    validUntil = it.validUntil,
                )
            }

    private fun isLocallyAdministered(mac: String): Boolean {
        val firstOctet = MacAddressNormalizer.normalize(mac).split(":").firstOrNull()?.toIntOrNull(16) ?: return false
        return firstOctet and 0x02 == 0x02
    }

    private fun loadNetworkClientViews(): List<NetworkClientViewDto> =
        listNetworkClientsUsecase
            .list()
            .map {
                NetworkClientViewDto(
                    macAddress = it.macAddress,
                    ipAddresses = it.ipAddresses,
                    hostname = it.hostname,
                    allowed = it.allowed,
                    isLocalAdmin = isLocallyAdministered(it.macAddress),
                )
            }

    private fun validateMac(mac: String): String? {
        if (mac.isBlank()) {
            return "admin.allowedMacs.error.required"
        }
        if (!MAC_ADDRESS_REGEX.matches(mac)) {
            return "admin.allowedMacs.error.invalid"
        }
        return null
    }

    private fun ensureAllowedMacAccess(user: UserIdentitySnapshot) {
        if (!user.can(UserRole::canManageAllowedMacs)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
    }
}
