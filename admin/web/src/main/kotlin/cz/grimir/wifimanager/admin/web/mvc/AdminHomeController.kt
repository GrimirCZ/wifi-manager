package cz.grimir.wifimanager.admin.web.mvc

import cz.grimir.wifimanager.admin.application.commands.CancelTicketCommand
import cz.grimir.wifimanager.admin.application.commands.CreateTicketCommand
import cz.grimir.wifimanager.admin.application.model.UserIdentity
import cz.grimir.wifimanager.admin.application.model.UserRole
import cz.grimir.wifimanager.admin.application.ports.FindTicketPort
import cz.grimir.wifimanager.admin.application.usecases.commands.CancelTicketUsecase
import cz.grimir.wifimanager.admin.application.usecases.commands.CreateTicketUsecase
import cz.grimir.wifimanager.admin.core.aggregates.Ticket
import cz.grimir.wifimanager.admin.core.exceptions.UserAlreadyHasActiveTickets
import cz.grimir.wifimanager.admin.web.AdminWifiProperties
import cz.grimir.wifimanager.admin.web.mvc.dto.CreateTicketRequestDto
import cz.grimir.wifimanager.admin.web.security.support.CurrentUser
import cz.grimir.wifimanager.shared.core.TicketId
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Controller
class AdminHomeController(
    private val createTicketUsecase: CreateTicketUsecase,
    private val cancelTicketUsecase: CancelTicketUsecase,
    private val findTicketPort: FindTicketPort,
    private val wifiProperties: AdminWifiProperties,
) {
    @GetMapping("/admin", "/admin/")
    fun index(
        @CurrentUser user: UserIdentity,
        htmxRequest: HtmxRequest,
        modelMap: ModelMap,
    ): String {
        populateModel(user, modelMap)

        modelMap.addAttribute("createTicket", CreateTicketRequestDto())

        if (htmxRequest.isHtmxRequest) {
            populateModel(user, modelMap)
            return "admin/fragments/ticket-panel :: ticketPanel"
        }

        return "admin/index"
    }

    @PostMapping("/admin/ticket")
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
        } catch (_: UserAlreadyHasActiveTickets) {
            redirectAttributes.addFlashAttribute("alreadyHasOpen", true)

            if (isHtmx) {
                populateModel(user, modelMap)
                modelMap.addAttribute("createTicket", CreateTicketRequestDto())

                return "admin/fragments/ticket-panel :: ticketPanel"
            }
        }

        return "redirect:/admin"
    }

    @GetMapping("/admin/ticket/{ticketId}/_ended-banner")
    fun ticketExpiredBanner(
        @CurrentUser user: UserIdentity,
        @PathVariable ticketId: UUID,
        @RequestParam(required = false) accessCode: String?,
        @RequestParam(required = false) createdAt: Long?,
        @RequestParam(required = false) validUntil: Long?,
        modelMap: ModelMap,
    ): String {
        val ticket = findTicketPort.findById(TicketId(ticketId))

        if (ticket != null && ticket.authorId.id != user.userId.id) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }

        val createdAtInstant = createdAt?.let(Instant::ofEpochSecond) ?: ticket?.createdAt
        val validUntilInstant = validUntil?.let(Instant::ofEpochSecond) ?: ticket?.validUntil

        modelMap.addAttribute("ticketExpired", true)
        modelMap.addAttribute("isAdmin", user.can(UserRole::canHaveMultipleTickets))
        modelMap.addAttribute("expiredAccessCode", accessCode ?: ticket?.accessCode ?: ticketId.toString())
        modelMap.addAttribute("expiredCreatedAt", createdAtInstant)
        modelMap.addAttribute("expiredValidUntil", validUntilInstant)

        return "admin/fragments/ticket-ended-banner :: ticketEndedBanner"
    }

    @DeleteMapping("/admin/ticket/{ticketId}")
    fun endTicketEarly(
        @CurrentUser user: UserIdentity,
        @PathVariable ticketId: UUID,
        redirectAttributes: RedirectAttributes,
        htmxRequest: HtmxRequest,
        modelMap: ModelMap,
    ): String {
        val isHtmx = htmxRequest.isHtmxRequest

        val ticket = findTicketPort.findById(TicketId(ticketId))

        if (ticket != null && ticket.authorId.id != user.userId.id) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }

        cancelTicketUsecase.cancel(CancelTicketCommand(TicketId(ticketId), user))

        redirectAttributes.addFlashAttribute("ticketDeleted", true)

        if (isHtmx) {
            modelMap.addAttribute("isAdmin", user.can(UserRole::canHaveMultipleTickets))
            modelMap.addAttribute("expiredAccessCode", ticket?.accessCode ?: ticketId.toString())
            modelMap.addAttribute("expiredCreatedAt", ticket?.createdAt)
            modelMap.addAttribute("expiredValidUntil", ticket?.validUntil)

            return "admin/fragments/ticket-ended-banner :: ticketEndedBanner"
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
