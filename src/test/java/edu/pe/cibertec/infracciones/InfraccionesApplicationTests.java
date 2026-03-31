package edu.pe.cibertec.infracciones;

import edu.pe.cibertec.infracciones.dto.PagoResponseDTO;
import edu.pe.cibertec.infracciones.model.*;
import edu.pe.cibertec.infracciones.repository.InfractorRepository;
import edu.pe.cibertec.infracciones.repository.MultaRepository;
import edu.pe.cibertec.infracciones.repository.PagoRepository;
import edu.pe.cibertec.infracciones.repository.VehiculoRepository;
import edu.pe.cibertec.infracciones.service.impl.InfractorServiceImpl;
import edu.pe.cibertec.infracciones.service.impl.MultaServiceImpl;
import edu.pe.cibertec.infracciones.service.impl.PagoServiceImpl;
import edu.pe.cibertec.infracciones.service.impl.VehiculoServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InfraccionesApplicationTests {

	@Test
	void contextLoads() {
	}
    @Mock
    private InfractorRepository infractorRepository;


	@Mock
	private MultaRepository multaRepository;

	@Mock
	private PagoRepository pagoRepository;

	@InjectMocks
	private MultaServiceImpl multaService;

	@InjectMocks
	private PagoServiceImpl pagoService;


	@Test
	@DisplayName("Verificar si un infractor se encuntra bloqueado con 2 multas vencidas y 3 pagadas")
	 void givenOffenderWith2OverdueAnd3PaidFines_whenVerifyBloqueo_thenOffenderIsNotBlocked(){
		Long idInfractor = 1L;
		Vehiculo vehiculo = new Vehiculo(1L,"000-000","Toyota",2015);
		Infractor infractor  = new Infractor(1L,"12345678","Juan Roberto","Mamani Condorcanqui","condorcanqui@gmail.com",false);
		List<Multa> multas  = List.of(
				new Multa(1L,"M001-455QW",850.00, LocalDate.of(2026,3,30),LocalDate.of(2026,4,5), EstadoMulta.PAGADA,infractor,vehiculo),
				new Multa(1L,"M002-455QW",459.00, LocalDate.of(2026,3,21),LocalDate.of(2026,3,31), EstadoMulta.PAGADA,infractor,vehiculo),
				new Multa(1L,"M003-455QW",379.00, LocalDate.of(2026,3,11),LocalDate.of(2026,3,25), EstadoMulta.PAGADA,infractor,vehiculo),
				new Multa(1L,"M004-455QW",1789.00, LocalDate.of(2026,3,15),LocalDate.of(2026,3,26), EstadoMulta.VENCIDA,infractor,vehiculo),
				new Multa(1L,"M005-455QW",398.00, LocalDate.of(2026,3,9),LocalDate.of(2026,3,22), EstadoMulta.VENCIDA,infractor,vehiculo)
		);

		when(multaRepository.findByInfractor_Id(idInfractor)).thenReturn(multas);

		Boolean bloqueado = multaService.verificarBloqueo(idInfractor);

		assertEquals(false,bloqueado);
		verify(multaRepository, times(1)).findByInfractor_Id(idInfractor);
		verify(infractorRepository, never()).save(any(Infractor.class));
	}

	@Test
	@DisplayName("Multa pendiente con fecha limite pasada cambia de estado a VENCIDA")
	void givenPendingFine_whenUpdateStatuses_thenFineChangesToExpired(){
     Long idMulta = 1L;
		Vehiculo vehiculo = new Vehiculo(1L,"000-000","Toyota",2015);
		Infractor infractor  = new Infractor(1L,"12345678","Juan Roberto","Mamani Condorcanqui","condorcanqui@gmail.com",false);
	    Optional<Multa> multa = Optional.of(new Multa(1L, "M006-455QW", 1900.00, LocalDate.of(2025, 12, 20), LocalDate.of(2026, 1, 1), EstadoMulta.PENDIENTE, infractor, vehiculo));

		when(multaRepository.findById(idMulta)).thenReturn(multa);
		when(multaRepository.save(any(Multa.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Multa multaEntity = multaService.actualizarEstados(idMulta);
		assertEquals(EstadoMulta.VENCIDA, multaEntity.getEstado());
		verify(multaRepository, times(1)).findById(idMulta);
		verify(multaRepository,times(1)).save(argThat(m -> m.getEstado() == EstadoMulta.VENCIDA));
	}
	@Test
	@DisplayName("Multa de 500 recien emitida es pagada, aplicando descuento")
	void givenUnexpiredFine_whenProcessPayment_thenApplyDiscountAndStatusChangesToPaid(){
		Long idMulta = 1L;

		Vehiculo vehiculo = new Vehiculo(1L,"000-000","Toyota",2015);
		Infractor infractor  = new Infractor(1L,"12345678","Juan Roberto","Mamani Condorcanqui","condorcanqui@gmail.com",false);
		Multa multa = new Multa(1L, "M006-455QW", 500.00, LocalDate.now(), LocalDate.of(2026, 4, 8), EstadoMulta.PENDIENTE, infractor, vehiculo);
		when(multaRepository.findById(idMulta)).thenReturn(Optional.of(multa));
		when(pagoRepository.save(any(Pago.class))).thenAnswer(i -> i.getArguments()[0]);
		PagoResponseDTO pago = pagoService.procesar_Pago(idMulta);

		assertEquals(400,pago.getMontoPagado());
		assertEquals(EstadoMulta.PAGADA, multa.getEstado());
		verify(pagoRepository,times(1)).save(any(Pago.class));
		verify(multaRepository,times(1)).save(multa);
	}

	@Test
	@DisplayName("Multa vencida hace dos dias tiene recargo de 75 soles, no aplica descuento")
	void givenFineExpiredTwoDaysAgo_whenProcessPayment_thenCapturePaymentWithSurcharge(){
       Long idMulta = 1L;

		Vehiculo vehiculo = new Vehiculo(1L,"000-000","Toyota",2015);
		Infractor infractor  = new Infractor(1L,"12345678","Juan Roberto","Mamani Condorcanqui","condorcanqui@gmail.com",false);
		Multa multa = new Multa("M006-455QW", 500.00, LocalDate.now().minusDays(12),LocalDate.now().minusDays(2), EstadoMulta.PENDIENTE, infractor, vehiculo);

		when(multaRepository.findById(idMulta)).thenReturn(Optional.of(multa));

		ArgumentCaptor<Pago> captor = ArgumentCaptor.forClass(Pago.class);
		pagoService.procesar_Pago(idMulta);
		verify(pagoRepository, times(1)).save(captor.capture());

		Pago pagoCapturado = captor.getValue();

		assertEquals(75.00,pagoCapturado.getRecargo());
		assertEquals(575.00,pagoCapturado.getMontoPagado());


	}
}
