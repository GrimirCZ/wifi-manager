package cz.grimir.wifimanager.admin.web.mvc

import cz.grimir.wifimanager.admin.application.commands.RequestUserDeviceDeauthorizationCommand
import cz.grimir.wifimanager.admin.application.queries.FindUserDevicesByUserIdQuery
import cz.grimir.wifimanager.admin.application.usecases.commands.RequestUserDeviceDeauthorizationUsecase
import cz.grimir.wifimanager.admin.application.usecases.queries.FindAllUserDevicesUsecase
import cz.grimir.wifimanager.admin.application.usecases.queries.FindUserDevicesByUserIdUsecase
import cz.grimir.wifimanager.admin.core.value.UserDevice
import cz.grimir.wifimanager.admin.web.mvc.dto.UserDeviceViewDto
import cz.grimir.wifimanager.shared.application.UserIdentitySnapshot
import cz.grimir.wifimanager.shared.core.UserId
import cz.grimir.wifimanager.shared.core.UserRole
import cz.grimir.wifimanager.shared.security.mvc.CurrentUser
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Controller
class AdminUserDeviceController(
    private val findUserDevicesByUserIdUsecase: FindUserDevicesByUserIdUsecase,
    private val findAllUserDevicesUsecase: FindAllUserDevicesUsecase,
    private val requestUserDeviceDeauthorizationUsecase: RequestUserDeviceDeauthorizationUsecase,
) {
    @GetMapping("/admin/devices")
    fun devicesPage(
        @CurrentUser user: UserIdentitySnapshot,
        @RequestParam(required = false, defaultValue = "mine") scope: String,
        modelMap: ModelMap,
    ): String {
        val resolvedScope = resolveScope(scope, user)
        populateModel(modelMap, user, resolvedScope)
        return "admin/devices"
    }

    @GetMapping("/admin/devices/_list")
    fun devicesList(
        @CurrentUser user: UserIdentitySnapshot,
        @RequestParam(required = false, defaultValue = "mine") scope: String,
        modelMap: ModelMap,
    ): String {
        val resolvedScope = resolveScope(scope, user)
        populateListModel(modelMap, user, resolvedScope, failedMac = null)
        return "admin/fragments/user-devices :: userDeviceList"
    }

    @PostMapping("/admin/devices/{mac}/_deauthorize")
    fun deauthorize(
        @CurrentUser user: UserIdentitySnapshot,
        @PathVariable mac: String,
        @RequestParam(required = false) userId: String?,
        @RequestParam(required = false, defaultValue = "mine") scope: String,
        modelMap: ModelMap,
    ): String {
        val resolvedScope = resolveScope(scope, user)
        val targetUserId =
            resolveTargetUserId(
                requestedUserId = userId,
                currentUser = user,
                scope = resolvedScope,
            )
        try {
            requestUserDeviceDeauthorizationUsecase.request(
                RequestUserDeviceDeauthorizationCommand(
                    userId = targetUserId,
                    deviceMac = mac,
                    requestedBy = user,
                ),
            )
            populateListModel(modelMap, user, resolvedScope, failedMac = null)
        } catch (_: RuntimeException) {
            populateListModel(modelMap, user, resolvedScope, failedMac = mac)
        }
        return "admin/fragments/user-devices :: userDeviceList"
    }

    private fun populateModel(
        modelMap: ModelMap,
        user: UserIdentitySnapshot,
        scope: String,
    ) {
        modelMap.addAttribute("canManageAllowedMacs", user.can(UserRole::canManageAllowedMacs))
        modelMap.addAttribute("canViewAllDevices", user.can(UserRole::canCancelOtherUsersTickets))
        populateListModel(modelMap, user, scope, failedMac = null)
    }

    private fun populateListModel(
        modelMap: ModelMap,
        user: UserIdentitySnapshot,
        scope: String,
        failedMac: String?,
    ) {
        val devices = loadDevices(user, scope)
        modelMap.addAttribute("deviceScope", scope)
        modelMap.addAttribute("devices", devices.map { mapToView(it, failedMac) }.sortedBy { it.mac })
        modelMap.addAttribute("canViewAllDevices", user.can(UserRole::canCancelOtherUsersTickets))
    }

    private fun loadDevices(
        user: UserIdentitySnapshot,
        scope: String,
    ): List<UserDevice> =
        if (scope == "all") {
            findAllUserDevicesUsecase.find()
        } else {
            findUserDevicesByUserIdUsecase.find(FindUserDevicesByUserIdQuery(user.userId))
        }

    private fun mapToView(
        device: UserDevice,
        failedMac: String?,
    ): UserDeviceViewDto =
        UserDeviceViewDto(
            userId = device.userId.id,
            mac = device.mac,
            name = device.name,
            hostname = device.hostname,
            isRandomized = device.isRandomized,
            authorizedAt = device.authorizedAt,
            lastSeenAt = device.lastSeenAt,
            disconnectError = failedMac == device.mac,
        )

    private fun resolveScope(
        scope: String,
        user: UserIdentitySnapshot,
    ): String {
        if (scope != "all") {
            return "mine"
        }
        if (!user.can(UserRole::canCancelOtherUsersTickets)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        return "all"
    }

    private fun resolveTargetUserId(
        requestedUserId: String?,
        currentUser: UserIdentitySnapshot,
        scope: String,
    ): UserId {
        if (scope != "all") {
            return currentUser.userId
        }
        val parsedUserId =
            try {
                requestedUserId?.let { UserId(UUID.fromString(it)) }
            } catch (_: IllegalArgumentException) {
                null
            } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        if (!currentUser.can(UserRole::canCancelOtherUsersTickets)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        return parsedUserId
    }
}
