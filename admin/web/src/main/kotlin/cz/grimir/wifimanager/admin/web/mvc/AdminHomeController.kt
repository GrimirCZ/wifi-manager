package cz.grimir.wifimanager.admin.web.mvc

import cz.grimir.wifimanager.admin.application.commands.CreateTicketCommand
import cz.grimir.wifimanager.admin.application.model.UserIdentity
import cz.grimir.wifimanager.admin.application.usecases.commands.CreateTicketUsecase
import cz.grimir.wifimanager.admin.core.exceptions.UserAlreadyHasActiveTickets
import cz.grimir.wifimanager.admin.web.mvc.dto.CreateTicketRequestDto
import cz.grimir.wifimanager.admin.web.security.support.CurrentUser
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.time.Duration

@Controller
class AdminHomeController(
    val createTicketUsecase: CreateTicketUsecase
) {
    @GetMapping("/admin", "/admin/")
    fun index(modelMap: ModelMap): String {
        modelMap.addAttribute("createTicket", CreateTicketRequestDto())

        return "admin/index"
    }

    @PostMapping("/admin/tickets")
    fun createTicket(
        @CurrentUser
        user: UserIdentity,
        @ModelAttribute request: CreateTicketRequestDto,
        redirectAttributes: RedirectAttributes,
    ): String {
        try {
            createTicketUsecase.create(
                CreateTicketCommand(
                    accessCode = null,
                    duration = Duration.ofMinutes(request.validityMinutes.toLong()),
                    user = user
                )
            )
        } catch (e: UserAlreadyHasActiveTickets) {
            redirectAttributes.addFlashAttribute("alreadyHasOpen", true)
        }

        redirectAttributes.addFlashAttribute("ticketCreated", true)
        redirectAttributes.addFlashAttribute("ticketValidityMinutes", request.validityMinutes)

        return "redirect:/admin"
    }

    @GetMapping("/admin/components")
    fun components(): String = "admin/components"
}
