package cz.grimir.wifimanager.admin.web.mvc

import cz.grimir.wifimanager.admin.application.commands.CreateTicketCommand
import cz.grimir.wifimanager.admin.application.model.UserIdentity
import cz.grimir.wifimanager.admin.application.model.UserRole
import cz.grimir.wifimanager.admin.application.ports.FindTicketPort
import cz.grimir.wifimanager.admin.application.usecases.commands.CreateTicketUsecase
import cz.grimir.wifimanager.admin.core.aggregates.Ticket
import cz.grimir.wifimanager.admin.core.exceptions.UserAlreadyHasActiveTickets
import cz.grimir.wifimanager.admin.web.AdminWifiProperties
import cz.grimir.wifimanager.admin.web.mvc.dto.CreateTicketRequestDto
import cz.grimir.wifimanager.admin.web.security.support.CurrentUser
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxRequest
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.time.Duration
import java.time.Instant

@Controller
class AdminHomeController(
    private val createTicketUsecase: CreateTicketUsecase,
    private val findTicketPort: FindTicketPort,
    private val wifiProperties: AdminWifiProperties,
) {
    @GetMapping("/admin", "/admin/")
    fun index(
        @CurrentUser user: UserIdentity,
        modelMap: ModelMap,
    ): String {
        populateModel(user, modelMap)

        modelMap.addAttribute("createTicket", CreateTicketRequestDto())

        return "admin/index"
    }

    @PostMapping("/admin/tickets")
    fun createTicket(
        @CurrentUser
        user: UserIdentity,
        @ModelAttribute request: CreateTicketRequestDto,
        redirectAttributes: RedirectAttributes,
        htmxRequest: HtmxRequest,
        modelMap: ModelMap,
    ): String {
        val isHtmx = htmxRequest.isHtmxRequest

        try {
            createTicketUsecase.create(
                CreateTicketCommand(
                    accessCode = null,
                    duration = Duration.ofMinutes(request.validityMinutes.toLong()),
                    user = user,
                ),
            )

            redirectAttributes.addFlashAttribute("ticketCreated", true)
            redirectAttributes.addFlashAttribute("ticketValidityMinutes", request.validityMinutes)

            if (isHtmx) {
                populateModel(user, modelMap)
                modelMap.addAttribute("createTicket", CreateTicketRequestDto())

                return "admin/fragments/ticket-panel :: ticketPanel"
            }
        } catch (e: UserAlreadyHasActiveTickets) {
            redirectAttributes.addFlashAttribute("alreadyHasOpen", true)

            if (isHtmx) {
                populateModel(user, modelMap)
                modelMap.addAttribute("createTicket", CreateTicketRequestDto())

                return "admin/fragments/ticket-panel :: ticketPanel"
            }
        }

        return "redirect:/admin"
    }

    @GetMapping("/admin/components")
    fun components(): String = "admin/components"

    private fun findActiveTickets(user: UserIdentity): List<Ticket> {
        val now = Instant.now()
        return findTicketPort
            .findByAuthorId(user.userId.id)
            .filter { it.isActive(now) }
            .sortedByDescending { it.createdAt }
    }

    private fun populateModel(
        user: UserIdentity,
        modelMap: ModelMap,
    ) {
        modelMap.addAttribute("activeTickets", findActiveTickets(user))
        modelMap.addAttribute("isAdmin", user.can(UserRole::canHaveMultipleTickets))
        modelMap.addAttribute("wifiSsid", wifiProperties.ssid)
    }
}
