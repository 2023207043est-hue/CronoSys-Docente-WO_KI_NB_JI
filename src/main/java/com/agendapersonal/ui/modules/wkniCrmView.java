package com.agendapersonal.ui.modules;

import com.agendapersonal.domain.model.wkniContacto;
import com.agendapersonal.infrastructure.repo.wkniContactoDAO;
import com.agendapersonal.service.wkniContactoService;
import com.agendapersonal.ui.wkniAnim;
import com.agendapersonal.ui.wkniIconFactory;
import com.agendapersonal.ui.wkniNotifier;
import com.agendapersonal.util.wkniUi;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.ColumnConstraints;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

/** Contactos (CRM personal): lista con avatares a la izquierda y ficha editable a la derecha. */
public class wkniCrmView {
    private static final int wkniDIAS_SEGUIMIENTO = 30;
    private final wkniContactoDAO wkniDao = new wkniContactoDAO();
    private final VBox wkniView;
    private final ListView<wkniContacto> wkniList = new ListView<>();
    private final TextField wkniBuscar = new TextField();
    private final wkniUi.wkniSegmented wkniFiltro = new wkniUi.wkniSegmented("Todos", "Favoritos", "Por contactar");
    private final TextField wkniNombre = new TextField();
    private final TextField wkniTel = new TextField();
    private final TextField wkniEmail = new TextField();
    private final ComboBox<String> wkniCategoria = new ComboBox<>();
    private final DatePicker wkni_cumple = new DatePicker();
    private final TextField wkniApodo = new TextField();
    private final VBox wkniFechasBox = new VBox(8);
    private final Label wkniFaltan = wkniUi.wkniLabel("", "pill", "pill-violet");
    private final TextArea wkniNotas = new TextArea();
    private final StackPane wkniAvatarBox = new StackPane();
    private final Label wkniNombreGrande = wkniUi.wkniLabel("", "h2");
    private final HBox wkniChips = new HBox(10);
    private final Button wkniFavBtn = wkniUi.wkniIconButton("favorite", "Marcar como favorito");
    private final Button wkniContactado = wkniUi.wkniSecondary("check", "Contactado hoy");
    private final Button wkniBorrar = wkniUi.wkniGhost("delete", "Eliminar contacto");
    private boolean wkniFavorito;
    private wkniContacto wkniActual;

    public wkniCrmView() {
        VBox wkniPage = wkniUi.wkniPage();
        Button wkniNuevo = wkniUi.wkniPrimary("add", "Nuevo contacto");
        wkniNuevo.setOnAction(wkniE -> wkniLimpiar());
        wkniPage.getChildren().add(wkniUi.wkniPageHeader("contacts", "Contactos",
            "Tus personas importantes y cuándo hablaste con ellas por última vez.", wkniNuevo));
        HBox wkniBody = new HBox(24, wkniPanelLista(), wkniPanelFicha());
        VBox.setVgrow(wkniBody, Priority.ALWAYS);
        wkniPage.getChildren().add(wkniBody);
        wkniView = wkniPage;
        wkniFill();
        wkniList.getSelectionModel().selectFirst();
        if (wkniList.getItems().isEmpty()) wkniLimpiar();
    }

    private VBox wkniPanelLista() {
        HBox wkniBusq = wkniUi.wkniSearchBox(wkniBuscar, "Buscar contactos");
        wkniBusq.getStyleClass().add("search-soft");
        wkniBuscar.textProperty().addListener((wkniO, wkniA, wkniB) -> wkniFill());
        wkniFiltro.wkniSelectedProperty().addListener((wkniO, wkniA, wkniB) -> wkniFill());
        wkniFiltro.wkniFillWidth();
        wkniList.getStyleClass().addAll("clean-list", "contact-list");
        wkniList.setCellFactory(wkniLv -> new wkniContactoCell());
        wkniList.setPlaceholder(wkniUi.wkniEmpty("contacts", "Sin contactos", "Agrega a las personas importantes con «Nuevo contacto»."));
        wkniList.getSelectionModel().selectedItemProperty().addListener((wkniO, wkniA, wkniB) -> { if (wkniB != null) wkniMostrar(wkniB); });
        VBox.setVgrow(wkniList, Priority.ALWAYS);
        VBox wkniTop = new VBox(12, wkniBusq, wkniFiltro.wkniNode());
        wkniTop.setPadding(new Insets(0, 6, 4, 6));
        VBox wkniCard = wkniUi.wkniCard();
        wkniCard.getStyleClass().add("side-panel");
        wkniCard.setPadding(new Insets(16, 10, 10, 10));
        wkniCard.setPrefWidth(340);
        wkniCard.setMinWidth(270);
        wkniCard.setMaxWidth(380);
        wkniCard.getChildren().addAll(wkniTop, wkniList);
        return wkniCard;
    }

    private final class wkniContactoCell extends ListCell<wkniContacto> {
        @Override protected void updateItem(wkniContacto wkniX, boolean wkniEmpty) {
            super.updateItem(wkniX, wkniEmpty);
            setText(null);
            if (wkniEmpty || wkniX == null) { setGraphic(null); return; }
            Label wkniN = new Label(wkniX.wkniGetNombre());
            wkniN.getStyleClass().add("list-title");
            String wkniCat = wkniX.wkniGetCategoria() == null ? "Personal" : wkniX.wkniGetCategoria();
            Label wkniSub = new Label(wkniCat + " · " + wkniUi.wkniHaceCuanto(wkniUltimo(wkniX)));
            wkniSub.getStyleClass().add("list-sub");
            VBox wkniT = new VBox(3, wkniN, wkniSub);
            HBox.setHgrow(wkniT, Priority.ALWAYS);
            HBox wkniRow = new HBox(12, wkniUi.wkniAvatar(wkniX.wkniGetNombre(), 40), wkniT);
            wkniRow.setAlignment(Pos.CENTER_LEFT);
            if (wkniNecesitaSeguimiento(wkniX)) wkniRow.getChildren().add(wkniUi.wkniPill("Escríbele", "amber"));
            else if (wkniX.wkniIsFavorito()) wkniRow.getChildren().add(wkniIconFactory.wkniIcon("favorite", 16, "tint-amber", "icon-fill"));
            setGraphic(wkniRow);
        }
    }

    private static LocalDate wkniUltimo(wkniContacto wkniX) {
        try { return wkniX.wkniGetUltimoContacto() == null || wkniX.wkniGetUltimoContacto().isBlank() ? null : LocalDate.parse(wkniX.wkniGetUltimoContacto()); }
        catch (Exception wkniE) { return null; }
    }

    private static boolean wkniNecesitaSeguimiento(wkniContacto wkniX) {
        OptionalLong wkniD = wkniContactoService.wkniDiasSinContacto(wkniX, LocalDate.now());
        return wkniD.isPresent() && wkniD.getAsLong() > wkniDIAS_SEGUIMIENTO;
    }

    private Node wkniPanelFicha() {
        wkniAvatarBox.setMinSize(64, 64);
        HBox wkniActions = new HBox(8, wkniContactado, wkniFavBtn);
        wkniActions.setAlignment(Pos.CENTER_RIGHT);
        VBox wkniNameBox = new VBox(8, wkniNombreGrande, wkniChips);
        wkniChips.setAlignment(Pos.CENTER_LEFT);
        HBox wkniHeader = new HBox(16, wkniAvatarBox, wkniNameBox, wkniUi.wkniSpacer(), wkniActions);
        wkniHeader.setAlignment(Pos.CENTER_LEFT);

        wkniCategoria.getItems().addAll("Familia", "Amigos", "Trabajo", "Clientes", "Personal");
        for (String wkniExtra : com.agendapersonal.settings.wkniSettingsStore.wkniGet().wkniStrings("contacts.categories"))
            if (!wkniCategoria.getItems().contains(wkniExtra)) wkniCategoria.getItems().add(wkniExtra);
        wkniCategoria.setEditable(true);          // se puede escribir una categoría propia
        wkniCategoria.setValue("Personal");
        wkniApodo.setPromptText("Cómo le dices (opcional)");
        wkniNombre.setPromptText("Nombre y apellido");
        wkniTel.setPromptText("+51 999 999 999");
        wkniEmail.setPromptText("correo@ejemplo.com");
        wkniUi.wkniFechaCorta(wkni_cumple);
        wkni_cumple.setPromptText("Elige la fecha");
        wkni_cumple.valueProperty().addListener((wkniO, wkniA, wkniB) -> wkniActualizarFaltan());
        wkniNotas.setPromptText("Contexto, gustos, de qué hablaron la última vez…");
        wkniNotas.setWrapText(true);
        wkniNotas.setPrefRowCount(5);

        GridPane wkniForm = new GridPane();
        wkniForm.setHgap(20);
        wkniForm.setVgap(16);
        ColumnConstraints wkniCol = new ColumnConstraints();
        wkniCol.setPercentWidth(50);
        wkniForm.getColumnConstraints().addAll(wkniCol, wkniCol);
        wkniForm.add(wkniUi.wkniField("Nombre", wkniNombre), 0, 0);
        wkniForm.add(wkniUi.wkniField("Categoría", wkniCategoria), 1, 0);
        wkniForm.add(wkniUi.wkniField("Teléfono", wkniConIcono("phone", wkniTel)), 0, 1);
        wkniForm.add(wkniUi.wkniField("Correo", wkniConIcono("mail", wkniEmail)), 1, 1);
        HBox wkniCumpleRow = new HBox(10, wkni_cumple, wkniFaltan);
        wkniCumpleRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(wkni_cumple, Priority.ALWAYS);
        wkni_cumple.setMaxWidth(Double.MAX_VALUE);
        wkniForm.add(wkniUi.wkniField("Cumpleaños", wkniCumpleRow), 0, 2);
        wkniForm.add(wkniUi.wkniField("Apodo", wkniApodo), 1, 2);
        Button wkniMasFecha = wkniUi.wkniSecondary(null, "+ Agregar fecha importante");
        wkniMasFecha.setOnAction(wkniE -> wkniFilaFecha("", null));
        wkniForm.add(wkniUi.wkniField("Otras fechas importantes (aniversario, graduación…)", new VBox(8, wkniFechasBox, wkniMasFecha)), 0, 3, 2, 1);
        VBox wkniNotasField = wkniUi.wkniField("Notas", wkniNotas);
        VBox.setVgrow(wkniNotas, Priority.ALWAYS);

        wkniContactado.setOnAction(wkniE -> {
            if (wkniActual == null) return;
            wkniContactoService.wkniMarcarContactadoHoy(wkniActual, LocalDate.now());
            if (wkniDao.wkniActualizar(wkniActual)) {
                wkniAnim.wkniPop(wkniContactado);
                wkniNotifier.wkniSuccess("Último contacto: hoy", "Registraste que hablaste con " + wkniActual.wkniGetNombre() + ".");
                wkniFill();
                wkniMostrar(wkniActual);
            }
        });
        wkniFavBtn.setOnAction(wkniE -> { wkniSetFav(!wkniFavorito); wkniAnim.wkniPop(wkniFavBtn); });
        wkniBorrar.getStyleClass().add("text-danger-btn");
        wkniBorrar.setOnAction(wkniE -> wkniEliminar());
        Button wkniCancelar = wkniUi.wkniSecondary(null, "Cancelar");
        wkniCancelar.setOnAction(wkniE -> { if (wkniActual != null) wkniMostrar(wkniActual); else wkniLimpiar(); });
        Button wkniGuardar = wkniUi.wkniPrimary("check", "Guardar cambios");
        wkniGuardar.setOnAction(wkniE -> wkniGuardar());
        HBox wkniFoot = new HBox(10, wkniBorrar, wkniUi.wkniSpacer(), wkniCancelar, wkniGuardar);
        wkniFoot.setAlignment(Pos.CENTER_LEFT);
        wkniFoot.getStyleClass().add("card-footer");

        Region wkniLinea = new Region();
        wkniLinea.getStyleClass().add("hairline");
        VBox wkniCard = wkniUi.wkniCard();
        wkniCard.setPadding(new Insets(24, 28, 18, 28));
        wkniCard.setSpacing(20);
        wkniCard.getChildren().addAll(wkniHeader, wkniLinea, wkniForm, wkniNotasField, wkniFoot);
        ScrollPane wkniSp = new ScrollPane(wkniCard);
        wkniSp.setFitToWidth(true);
        wkniSp.setFitToHeight(true);
        wkniSp.getStyleClass().add("page-scroll");
        HBox.setHgrow(wkniSp, Priority.ALWAYS);
        return wkniSp;
    }

    private static HBox wkniConIcono(String wkniIcon, TextField wkniF) {
        wkniF.getStyleClass().add("bare-field");
        HBox.setHgrow(wkniF, Priority.ALWAYS);
        HBox wkniBox = new HBox(10, wkniIconFactory.wkniIcon(wkniIcon, 16), wkniF);
        wkniBox.getStyleClass().add("input-with-icon");
        wkniBox.setAlignment(Pos.CENTER_LEFT);
        wkniF.focusedProperty().addListener((wkniO, wkniA, wkniB) ->
            wkniBox.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("focused"), wkniB));
        return wkniBox;
    }

    private void wkniSetFav(boolean wkniOn) {
        wkniFavorito = wkniOn;
        wkniFavBtn.setGraphic(wkniIconFactory.wkniIcon("favorite", 18, wkniOn ? "tint-amber" : "tint-none", wkniOn ? "icon-fill" : "no-fill"));
        wkniFavBtn.getStyleClass().remove("fav-on");
        if (wkniOn) wkniFavBtn.getStyleClass().add("fav-on");
        wkniUi.wkniTooltip(wkniFavBtn, wkniOn ? "Quitar de favoritos" : "Marcar como favorito");
    }

    private void wkniActualizarFaltan() {
        LocalDate wkniD = wkni_cumple.getValue();
        if (wkniD == null) { wkniFaltan.setVisible(false); wkniFaltan.setManaged(false); return; }
        LocalDate wkniHoy = LocalDate.now();
        LocalDate wkniProx = wkniD.withYear(wkniHoy.getYear());
        if (wkniProx.isBefore(wkniHoy)) wkniProx = wkniProx.plusYears(1);
        long wkniDias = ChronoUnit.DAYS.between(wkniHoy, wkniProx);
        wkniFaltan.setText(wkniDias == 0 ? "¡Es hoy!" : wkniDias == 1 ? "Mañana" : "Faltan " + wkniDias + " días");
        wkniFaltan.setVisible(true);
        wkniFaltan.setManaged(true);
    }

    private void wkniCabecera(String wkniN, String wkniCat, LocalDate wkniUlt, boolean wkniNuevo) {
        wkniAvatarBox.getChildren().setAll(wkniNuevo ? wkniUi.wkniIconChip("contacts", "slate", 64, 28) : wkniUi.wkniAvatar(wkniN, 64));
        wkniNombreGrande.setText(wkniNuevo ? "Nuevo contacto" : wkniN);
        wkniChips.getChildren().clear();
        if (!wkniNuevo) {
            wkniChips.getChildren().add(wkniUi.wkniPill(wkniCat, "blue"));
            Label wkniU = wkniUi.wkniLabel(wkniUlt == null ? "Aún sin registrar contacto" : "Último contacto " + wkniUi.wkniHaceCuanto(wkniUlt), "muted");
            wkniU.setGraphic(wkniIconFactory.wkniIcon("clock", 14));
            wkniU.setGraphicTextGap(6);
            wkniChips.getChildren().add(wkniU);
        } else wkniChips.getChildren().add(wkniUi.wkniMuted("Completa los datos y pulsa «Guardar cambios»."));
        wkniContactado.setDisable(wkniNuevo);
        wkniBorrar.setDisable(wkniNuevo);
    }

    private void wkniFill() {
        String wkniQ = wkniBuscar.getText() == null ? "" : wkniBuscar.getText().trim().toLowerCase(wkniUi.wkniES);
        int wkniF = wkniFiltro.wkniSelected();
        List<wkniContacto> wkniTodos = wkniDao.wkniListar();
        wkniContacto wkniSel = wkniList.getSelectionModel().getSelectedItem();
        wkniList.getItems().setAll(wkniTodos.stream()
            .filter(wkniX -> wkniF == 0 || (wkniF == 1 && wkniX.wkniIsFavorito()) || (wkniF == 2 && wkniNecesitaSeguimiento(wkniX)))
            .filter(wkniX -> wkniQ.isEmpty() || wkniX.wkniGetNombre().toLowerCase(wkniUi.wkniES).contains(wkniQ)
                || (wkniX.wkniGetEmail() != null && wkniX.wkniGetEmail().toLowerCase(wkniUi.wkniES).contains(wkniQ))
                || (wkniX.wkniGetTelefono() != null && wkniX.wkniGetTelefono().contains(wkniQ)))
            .toList());
        if (wkniSel != null) for (wkniContacto wkniX : wkniList.getItems()) if (wkniX.wkniGetId() == wkniSel.wkniGetId()) { wkniList.getSelectionModel().select(wkniX); break; }
    }

    private void wkniMostrar(wkniContacto wkniX) {
        wkniActual = wkniX;
        wkniNombre.setText(wkniX.wkniGetNombre());
        wkniTel.setText(wkniX.wkniGetTelefono() == null ? "" : wkniX.wkniGetTelefono());
        wkniEmail.setText(wkniX.wkniGetEmail() == null ? "" : wkniX.wkniGetEmail());
        String wkniCatX = wkniX.wkniGetCategoria() == null ? "Personal" : wkniX.wkniGetCategoria();
        if (!wkniCategoria.getItems().contains(wkniCatX)) wkniCategoria.getItems().add(wkniCatX);
        wkniCategoria.setValue(wkniCatX);
        wkniApodo.setText(wkniX.wkniGetApodo() == null ? "" : wkniX.wkniGetApodo());
        wkniFechasBox.getChildren().clear();
        for (var wkniF : com.agendapersonal.service.wkniFechasImportantes.wkniLeer(wkniX.wkniGetFechas())) wkniFilaFecha(wkniF.wkniEtiqueta(), wkniF.wkni_fecha());
        try { wkni_cumple.setValue(wkniX.wkniGetCumpleanos() == null || wkniX.wkniGetCumpleanos().isBlank() ? null : LocalDate.parse(wkniX.wkniGetCumpleanos())); }
        catch (Exception wkniIgnored) { wkni_cumple.setValue(null); }
        // Antes el "Último contacto" se pegaba dentro de las notas y se duplicaba en cada guardado.
        wkniNotas.setText(wkniX.wkniGetNotas() == null ? "" : wkniX.wkniGetNotas());
        wkniSetFav(wkniX.wkniIsFavorito());
        wkniCabecera(wkniX.wkniGetNombre(), wkniCategoria.getValue(), wkniUltimo(wkniX), false);
        wkniActualizarFaltan();
    }

    private void wkniLimpiar() {
        wkniActual = null;
        wkniList.getSelectionModel().clearSelection();
        wkniNombre.clear(); wkniTel.clear(); wkniEmail.clear(); wkniNotas.clear();
        wkniCategoria.setValue("Personal");
        wkni_cumple.setValue(null);
        wkniApodo.clear();
        wkniFechasBox.getChildren().clear();
        wkniSetFav(false);
        wkniCabecera(null, null, null, true);
        wkniActualizarFaltan();
        wkniNombre.requestFocus();
    }

    /** Agrega una fila (etiqueta + fecha + quitar) a las fechas importantes del contacto. */
    private void wkniFilaFecha(String wkniEtiqueta, LocalDate wkni_fecha) {
        TextField wkniEt = new TextField(wkniEtiqueta);
        wkniEt.setPromptText("Ej. Aniversario");
        DatePicker wkniDp = new DatePicker(wkni_fecha);
        wkniUi.wkniFechaCorta(wkniDp);
        Button wkniQuitar = wkniUi.wkniIconButton("close", "Quitar fecha");
        HBox wkniFila = new HBox(8, wkniEt, wkniDp, wkniQuitar);
        wkniFila.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(wkniEt, Priority.ALWAYS);
        wkniQuitar.setOnAction(wkniE -> wkniFechasBox.getChildren().remove(wkniFila));
        wkniFechasBox.getChildren().add(wkniFila);
    }

    private String wkniLeerFechas() {
        List<com.agendapersonal.service.wkniFechasImportantes.wkniFecha> wkniL = new ArrayList<>();
        for (Node wkniN : wkniFechasBox.getChildren())
            if (wkniN instanceof HBox wkniH && wkniH.getChildren().get(0) instanceof TextField wkniT && wkniH.getChildren().get(1) instanceof DatePicker wkniD && wkniD.getValue() != null)
                wkniL.add(new com.agendapersonal.service.wkniFechasImportantes.wkniFecha(wkniT.getText(), wkniD.getValue()));
        return com.agendapersonal.service.wkniFechasImportantes.wkniEscribir(wkniL);
    }

    /** Recuerda las categorías propias para ofrecerlas la próxima vez. */
    private void wkniGuardarCategoria(String wkniCat) {
        var wkniSt = com.agendapersonal.settings.wkniSettingsStore.wkniGet();
        List<String> wkniBase = List.of("Familia", "Amigos", "Trabajo", "Clientes", "Personal");
        List<String> wkniPropias = new ArrayList<>(wkniSt.wkniStrings("contacts.categories"));
        if (!wkniBase.contains(wkniCat) && !wkniPropias.contains(wkniCat)) { wkniPropias.add(wkniCat); wkniSt.wkniSet("contacts.categories", wkniPropias); }
        if (!wkniCategoria.getItems().contains(wkniCat)) wkniCategoria.getItems().add(wkniCat);
    }

    private void wkniGuardar() {
        if (wkniNombre.getText().isBlank()) {
            wkniAnim.wkniShake(wkniNombre);
            wkniNotifier.wkniWarning("Falta el nombre", "Escribe el nombre del contacto para guardarlo.");
            return;
        }
        String wkniCum = wkni_cumple.getValue() == null ? null : wkni_cumple.getValue().toString();
        String wkniN = wkniNombre.getText().trim();
        String wkniCat = wkniCategoria.getEditor().getText() == null || wkniCategoria.getEditor().getText().isBlank() ? "Personal" : wkniCategoria.getEditor().getText().trim();
        wkniCategoria.setValue(wkniCat);
        wkniGuardarCategoria(wkniCat);
        String wkniFechasTxt = wkniLeerFechas();
        boolean wkniOk;
        boolean wkniEsNuevo = wkniActual == null;
        if (wkniEsNuevo) {
            wkniContacto wkniNuevo = new wkniContacto(0, wkniN, wkniTel.getText().trim(), wkniEmail.getText().trim(),
                wkniCat, wkniCum, wkniNotas.getText(), wkniFavorito, null);
            wkniNuevo.wkniSetApodo(wkniApodo.getText().trim());
            wkniNuevo.wkniSetFechas(wkniFechasTxt);
            wkniOk = wkniDao.wkniInsertar(wkniNuevo);
        } else {
            wkniActual.wkniSetNombre(wkniN);
            wkniActual.wkniSetTelefono(wkniTel.getText().trim());
            wkniActual.wkniSetEmail(wkniEmail.getText().trim());
            wkniActual.wkniSetCategoria(wkniCat);
            wkniActual.wkniSetApodo(wkniApodo.getText().trim());
            wkniActual.wkniSetFechas(wkniFechasTxt);
            wkniActual.wkniSetCumpleanos(wkniCum);
            wkniActual.wkniSetNotas(wkniNotas.getText());
            wkniActual.wkniSetFavorito(wkniFavorito);
            wkniOk = wkniDao.wkniActualizar(wkniActual);
        }
        if (!wkniOk) { wkniNotifier.wkniError("No se pudo guardar el contacto", null); return; }
        wkniFill();
        for (wkniContacto wkniX : wkniList.getItems()) if (wkniX.wkniGetNombre().equals(wkniN)) { wkniList.getSelectionModel().select(wkniX); wkniMostrar(wkniX); break; }
        wkniNotifier.wkniSuccess(wkniEsNuevo ? "Contacto agregado" : "Contacto actualizado", wkniN);
    }

    private void wkniEliminar() {
        if (wkniActual == null) return;
        if (!wkniUi.wkniConfirm(wkniView, "¿Eliminar a " + wkniActual.wkniGetNombre() + "?", "Se borrará su ficha y sus notas.", "Eliminar")) return;
        String wkniN = wkniActual.wkniGetNombre();
        if (wkniDao.wkniEliminar(wkniActual.wkniGetId())) {
            wkniFill();
            if (wkniList.getItems().isEmpty()) wkniLimpiar(); else wkniList.getSelectionModel().selectFirst();
            wkniNotifier.wkniDeleted("Contacto eliminado", wkniN);
        }
    }

    public VBox wkniGetView() { return wkniView; }
}
