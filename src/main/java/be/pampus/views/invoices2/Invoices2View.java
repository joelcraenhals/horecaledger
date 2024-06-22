package be.pampus.views.invoices2;

import be.pampus.data.DeliveryNote;
import be.pampus.services.DeliveryNoteService;
import be.pampus.views.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@PageTitle("Invoices2")
@Route(value = "invoices2/:deliveryNoteID?/:action?(edit)", layout = MainLayout.class)
@AnonymousAllowed
public class Invoices2View extends Div implements BeforeEnterObserver {

    private final String DELIVERYNOTE_ID = "deliveryNoteID";
    private final String DELIVERYNOTE_EDIT_ROUTE_TEMPLATE = "invoices2/%s/edit";

    private final Grid<DeliveryNote> grid = new Grid<>(DeliveryNote.class, false);

    private DatePicker date;
    private Upload price;
    private Image pricePreview;

    private final Button cancel = new Button("Cancel");
    private final Button save = new Button("Save");

    private final BeanValidationBinder<DeliveryNote> binder;

    private DeliveryNote deliveryNote;

    private final DeliveryNoteService deliveryNoteService;

    public Invoices2View(DeliveryNoteService deliveryNoteService) {
        this.deliveryNoteService = deliveryNoteService;
        addClassNames("invoices2-view");

        // Create UI
        SplitLayout splitLayout = new SplitLayout();

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);

        // Configure Grid
        grid.addColumn("date").setAutoWidth(true);
        LitRenderer<DeliveryNote> priceRenderer = LitRenderer
                .<DeliveryNote>of("<img style='height: 64px' src=${item.price} />").withProperty("price", item -> {
                    if (item != null && item.getPrice() != null) {
                        return "data:image;base64," + Base64.getEncoder().encodeToString(item.getPrice());
                    } else {
                        return "";
                    }
                });
        grid.addColumn(priceRenderer).setHeader("Price").setWidth("68px").setFlexGrow(0);

        grid.setItems(query -> deliveryNoteService.list(
                PageRequest.of(query.getPage(), query.getPageSize(), VaadinSpringDataHelpers.toSpringDataSort(query)))
                .stream());
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(DELIVERYNOTE_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm();
                UI.getCurrent().navigate(Invoices2View.class);
            }
        });

        // Configure Form
        binder = new BeanValidationBinder<>(DeliveryNote.class);

        // Bind fields. This is where you'd define e.g. validation rules

        binder.bindInstanceFields(this);

        attachImageUpload(price, pricePreview);

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
        });

        save.addClickListener(e -> {
            try {
                if (this.deliveryNote == null) {
                    this.deliveryNote = new DeliveryNote();
                }
                binder.writeBean(this.deliveryNote);
                deliveryNoteService.update(this.deliveryNote);
                clearForm();
                refreshGrid();
                Notification.show("Data updated");
                UI.getCurrent().navigate(Invoices2View.class);
            } catch (ObjectOptimisticLockingFailureException exception) {
                Notification n = Notification.show(
                        "Error updating the data. Somebody else has updated the record while you were making changes.");
                n.setPosition(Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (ValidationException validationException) {
                Notification.show("Failed to update the data. Check again that all values are valid");
            }
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> deliveryNoteId = event.getRouteParameters().get(DELIVERYNOTE_ID).map(Long::parseLong);
        if (deliveryNoteId.isPresent()) {
            Optional<DeliveryNote> deliveryNoteFromBackend = deliveryNoteService.get(deliveryNoteId.get());
            if (deliveryNoteFromBackend.isPresent()) {
                populateForm(deliveryNoteFromBackend.get());
            } else {
                Notification.show(
                        String.format("The requested deliveryNote was not found, ID = %s", deliveryNoteId.get()), 3000,
                        Notification.Position.BOTTOM_START);
                // when a row is selected but the data is no longer available,
                // refresh grid
                refreshGrid();
                event.forwardTo(Invoices2View.class);
            }
        }
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        Div editorLayoutDiv = new Div();
        editorLayoutDiv.setClassName("editor-layout");

        Div editorDiv = new Div();
        editorDiv.setClassName("editor");
        editorLayoutDiv.add(editorDiv);

        FormLayout formLayout = new FormLayout();
        date = new DatePicker("Date");
        NativeLabel priceLabel = new NativeLabel("Price");
        pricePreview = new Image();
        pricePreview.setWidth("100%");
        price = new Upload();
        price.getStyle().set("box-sizing", "border-box");
        price.getElement().appendChild(pricePreview.getElement());
        formLayout.add(date, priceLabel, price);

        editorDiv.add(formLayout);
        createButtonLayout(editorLayoutDiv);

        splitLayout.addToSecondary(editorLayoutDiv);
    }

    private void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(save, cancel);
        editorLayoutDiv.add(buttonLayout);
    }

    private void createGridLayout(SplitLayout splitLayout) {
        Div wrapper = new Div();
        wrapper.setClassName("grid-wrapper");
        splitLayout.addToPrimary(wrapper);
        wrapper.add(grid);
    }

    private void attachImageUpload(Upload upload, Image preview) {
        ByteArrayOutputStream uploadBuffer = new ByteArrayOutputStream();
        upload.setAcceptedFileTypes("image/*");
        upload.setReceiver((fileName, mimeType) -> {
            uploadBuffer.reset();
            return uploadBuffer;
        });
        upload.addSucceededListener(e -> {
            StreamResource resource = new StreamResource(e.getFileName(),
                    () -> new ByteArrayInputStream(uploadBuffer.toByteArray()));
            preview.setSrc(resource);
            preview.setVisible(true);
            if (this.deliveryNote == null) {
                this.deliveryNote = new DeliveryNote();
            }
            this.deliveryNote.setPrice(uploadBuffer.toByteArray());
        });
        preview.setVisible(false);
    }

    private void refreshGrid() {
        grid.select(null);
        grid.getDataProvider().refreshAll();
    }

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(DeliveryNote value) {
        this.deliveryNote = value;
        binder.readBean(this.deliveryNote);
        this.pricePreview.setVisible(value != null);
        if (value == null || value.getPrice() == null) {
            this.price.clearFileList();
            this.pricePreview.setSrc("");
        } else {
            this.pricePreview.setSrc("data:image;base64," + Base64.getEncoder().encodeToString(value.getPrice()));
        }

    }
}
