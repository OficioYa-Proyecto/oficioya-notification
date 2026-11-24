package oficioya_notification.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import oficioya_notification.dto.EmailRequest;
import oficioya_notification.dto.NotificationRequest;
import oficioya_notification.dto.NotificationResponse;
import oficioya_notification.dto.ReviewReportRequest;
import oficioya_notification.service.EmailNotificationService;
import oficioya_notification.service.NotificationService;
import oficioya_notification.service.ReviewReportNotificationService;

@RestController 
@RequestMapping("/api/v1/notification")
@Tag(name="Notifications", description = "API to control the notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final ReviewReportNotificationService reviewReportNotificationService;
    private final EmailNotificationService emailNotificationService;

    public NotificationController(NotificationService notificationService, ReviewReportNotificationService reviewReportNotificationService, EmailNotificationService emailNotificationService){
        this.notificationService = notificationService;
        this.reviewReportNotificationService = reviewReportNotificationService;
        this.emailNotificationService = emailNotificationService;
    }

    @PostMapping 
    @Operation(summary="Send a notification", description = "Sends a notification and stores it in the database")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationResponse> send(@Valid @RequestBody NotificationRequest notification){
        NotificationResponse response = notificationService.sendNotification(notification);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get a notification by its id", description = "Gets the dto of the notification by the id given")
    @PreAuthorize("hasAnyRole('CONTRATANTE', 'TRABAJADOR')")
    public ResponseEntity<NotificationResponse> getById(@PathVariable UUID id){
        Optional<NotificationResponse> found = notificationService.findById(id);
        
        if(!found.isEmpty()){
            return ResponseEntity.status(HttpStatus.OK).body(found.get());
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @GetMapping("/user/{id}")
    @Operation(summary = "Get the notifications of a user", description = "Gets the notifications of a user by the id of the user")
    @PreAuthorize("hasAnyRole('CONTRATANTE', 'TRABAJADOR')")
    public ResponseEntity<List<NotificationResponse>> getAllUserNotifications(@PathVariable UUID id){
        /*
        Lógica de buscar id de usuario con servicio de usuarios
         */
        return ResponseEntity.status(HttpStatus.OK).body(notificationService.findAllForUser(id));
    }

    @GetMapping("/user/{userId}/request/{requestId}")
    @Operation(summary="Get the notification of a request", description = "Gets the notifications of a user associated with a notification")
    @PreAuthorize("hasAnyRole('CONTRATANTE', 'TRABAJADOR')")
    public ResponseEntity<List<NotificationResponse>> getAllUserNotificationsForARequest(@PathVariable UUID userId, @PathVariable UUID requestId){
        /*
        Lógica de buscar el id de usuario con servicio de usuarios
         */
        return ResponseEntity.status(HttpStatus.OK).body(notificationService.findAllForARequest(requestId, userId));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary="Delete a notification", description="Deletes a notification with its id, only an admin can do it")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteNotification(@PathVariable UUID id){
        boolean could = notificationService.deleteNotification(id);

        if(could){
            return ResponseEntity.status(HttpStatus.OK).body("Notification deleted");
        }
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Notification not found");
    }

    @PostMapping("/review-reported")
    @Operation(summary = "Notificar reseña reportada", description = "Recibe los datos de una reseña reportada y envía un correo al administrador. Responde confirmación de recepción.")
    public ResponseEntity<String> notifyReviewReported(@Valid @RequestBody ReviewReportRequest request) {
        String result = reviewReportNotificationService.notifyReviewReported(request);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PostMapping("/email")
    @Operation(summary = "Enviar correo", description = "Envía un correo al destinatario indicado (OTP, cambio de contraseña, etc.). Responde confirmación de recepción.")
    public ResponseEntity<String> sendEmail(@Valid @RequestBody EmailRequest request) {
        String result = emailNotificationService.sendEmail(request);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

}
