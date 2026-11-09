# Pruebas de sala privada: estructura de la práctica TDD

Las 18 clases de `src/test/java/epn/edu/ec/salas` usan JUnit 4, como la
calculadora de la práctica. Conservan 72 métodos de prueba y 89 ejecuciones
al incluir los 18 casos de la prueba parametrizada de admisión.

## Ciclo de vida

Cada clase extiende `PruebaSalaPrivada`, que declara los hooks compartidos:

- `@BeforeClass setUpClass()`: anuncia el inicio una vez por clase.
- `@Before setUp()`: prepara los objetos, mocks o la base H2 de cada caso.
- `@Test given_..._when_..._then_...()`: describe condición, acción y resultado.
- `@After tearDown()`: limpia las referencias o cierra la base de cada caso.
- `@AfterClass tearDownClass()`: anuncia el final una vez por clase.

JUnit 4 hereda los hooks de clase. En las pruebas con Mockito, `tearDown()`
también llama a `validateMockitoUsage()`. Los anuncios de inicio y fin son
didácticos; la preparación y liberación de recursos ocurre en cada caso.

Ejemplo: `given_solicitud_pendiente_when_responsable_acepta_then_solicitud_aceptada`
prepara una solicitud pendiente en `setUp()`, la acepta en el test, comprueba
el estado y limpia las referencias en `tearDown()`.

## Tipos de prueba

- Dominio y reglas: sala, solicitud, decisión, cierre, ciclo y política de admisión.
- Servicios aislados: `ServicioSalasPrivadasTest`, con repositorios en memoria.
- Mockito: `CreacionSalaMockTest` y `TransaccionesJpaMockTest`, con verificación
  de llamadas, argumentos y orden de las operaciones.
- Parametrizadas: `PoliticaAdmisionSalaParametersTest`, con `@RunWith(Parameterized.class)`
  y datos de fase, ocupación, rol y resultado esperado.
- Serialización: protocolo JSON del ciclo y del chat.
- Integración: las seis clases `*PersistenciaTest` usan JPA y H2 en memoria,
  incluyendo rollback y concurrencia. Se conservan como pruebas de integración.

## Ejecución

Con Maven y JDK 21 configurados, desde la raíz de Pomora:

```powershell
mvn "-Dtest=epn.edu.ec.salas.*Test" test
mvn test
```

Esta refactorización conserva los escenarios existentes. Los nombres y hooks
facilitan leer las pruebas con el estilo de la práctica; por sí solos no
demuestran un proceso histórico de desarrollo rojo, verde y refactorización.
