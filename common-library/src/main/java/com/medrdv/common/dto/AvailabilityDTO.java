tipackage com.medrdv.common.dto;

import com.medrdv.common.enums.SlotStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityDTO {
    private Long id;
    private Long doctorId;
    private LocalDateTime dateHeure;
    private Integer dureeMinutes;
    private SlotStatus statut;
    private Long version;
}
