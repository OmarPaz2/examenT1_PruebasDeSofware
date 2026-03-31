package edu.pe.cibertec.infracciones.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "multas")
public class Multa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private Double monto;

    @Column(nullable = false)
    private LocalDate fechaEmision;

    @Column(nullable = false)
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoMulta estado;

    @ManyToOne
    @JoinColumn(name = "infractor_id", nullable = false)
    private Infractor infractor;

    @ManyToOne
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @ManyToMany
    @JoinTable(
            name = "multa_tipo_infraccion",
            joinColumns = @JoinColumn(name = "multa_id"),
            inverseJoinColumns = @JoinColumn(name = "tipo_infraccion_id")
    )
    private List<TipoInfraccion> tiposInfraccion;

    public Multa(Long id, String codigo, Double monto, LocalDate fechaEmision, LocalDate fechaVencimiento, EstadoMulta estado, Infractor infractor, Vehiculo vehiculo) {
        this.id = id;
        this.codigo = codigo;
        this.monto = monto;
        this.fechaEmision = fechaEmision;
        this.fechaVencimiento = fechaVencimiento;
        this.estado = estado;
        this.infractor = infractor;
        this.vehiculo = vehiculo;
    }
}