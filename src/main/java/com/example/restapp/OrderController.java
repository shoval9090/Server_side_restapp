package com.example.restapp;

import com.example.restapp.Product;
import com.example.restapp.ProductController;
import org.springframework.context.annotation.Profile;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
public class OrderController {

    private final OrderRepo orderDatabase;
    private OrderEntityFactory orderEntityFactory;

    public OrderController(OrderRepo orderDatabase, OrderEntityFactory orderEntityFactory) {
        this.orderDatabase = orderDatabase;
        this.orderEntityFactory = orderEntityFactory;
    }

    // 3. הצגת כל ה products מתוך id אשר נמצאים בתוך -> orders
    @GetMapping("/orders/{id}/products")
    public ResponseEntity<CollectionModel<EntityModel<Product>>>
    productsByOrder(@PathVariable long id) {
        /*
         נמצא id מתוך orderDatabase ונשמור אותו בתוך ->  order,
         ואם לא נמצא תופעל פונקצית OrderNotFoundException(תחזיר שגיאה)
        */
        Order order = orderDatabase.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        // ניקח את כל המוצרים אשר נמצאים בתוך order ונשמור אותם ברשימה -> products
        List<Product> products = order.getProductsList();

        // נשמור את המוצרים דרך EntityModel, וגם נעשה קישור של המוצרים ושל כל ההזמנות
        List<EntityModel<Product>> ListProducts = products.stream()
                .map(product -> orderEntityFactory.ForProductModelWithOrderLink(order, product))
                .collect(Collectors.toList());

        // נחזיר באמצעות ResponseEntity את כל המוצרים + הוספה של סטאטוס 200
        return ResponseEntity.ok(CollectionModel.of(ListProducts));
    }




    // 1. הצגת כל orders
    @GetMapping("/orders")
    public ResponseEntity<CollectionModel<EntityModel<Order>>> getAllOrders() {
        // מכניס לנו את כל ה Data בתוך allOrders מ -> orderDatabase
        List<EntityModel<Order>> allOrders = orderDatabase.findAll().stream()
                .map(order -> orderEntityFactory.toModel(order))
                .collect(Collectors.toList());

        // נחזיר באמצעות ResponseEntity את כל ה orders וגם מוסיף לנו סטאטוס 200
        return ResponseEntity.ok(CollectionModel.of(allOrders));
    }


    // 2. הצגת id המבוקש מתוך orders, במידה ולא נמצא הID המבוקש, תופעל פונקציית == OrderNotFoundException
    @GetMapping("order/{id}")
    ResponseEntity<EntityModel<Order>> singleOrder(@PathVariable Long id) {
       /*
        מציאת המוצר על ידי ה  id שלו, אשר נמצא בתוך order מתוך orderDatabase,
        ושומר אותו ב idOrder, במידה ולא נמצא את המוצר המבוקש , תופעל הפונקציה == OrderNotFoundException(אשר מחזירה שגיאה)
        */
        Order idOrder = orderDatabase.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        // נחזיר באמצעות -- ResponseEntity את ה idOder והאישורים וגם נוסיף סטאטוס 200
        return ResponseEntity.ok(orderEntityFactory.toModel(idOrder));
    }

    // 3. הוספת הזמנה חדשה ל -> orders
    @PostMapping("/orders")
    // TODO: check why "?"
    ResponseEntity<?> placeOrder(@RequestBody Order newOrder) {
        //שומר newOrder בתוך orderDatabase
        EntityModel<Order> OrderRepresentation =
                orderEntityFactory.toModel(orderDatabase.save(newOrder));
        return ResponseEntity
                .created(OrderRepresentation.getRequiredLink(IanaLinkRelations.SELF).toUri())
                .body(OrderRepresentation);
    }


    // 4. עדכון המוצר ה id בתוך -> order
    @PutMapping("order/{id}")
    ResponseEntity<?> addToOrder(@RequestBody Product aProduct, @PathVariable(value = "id") Long orderID) {

        // נמצא orderId מתוך orderDatabase ונשמור אותו בתוך -> updatedOrder
        Order updatedOrder = orderDatabase.getById(orderID);
        // נוסיף המוצר אשר קראנו לו -- aProduct
        // " שהוא מגיע מצד לקוח -> RequestBody " לתוך מערך List שיצרנו
        // כל זה נמצא בתוך -> updatedOrder
        updatedOrder.getProductsList().add(aProduct);

        //שומר את updatedOrder בתוך orderDatabase
        EntityModel<Order> OrderRepresentation = orderEntityFactory.toModel(orderDatabase.save(updatedOrder));
        // מחזיר בסוף דרך ResponseEntity משפט וגם מוסיף סטאטוס 200
        return ResponseEntity.ok("Product was added successfully");
    }


    // 6. הצגת כל orders
    @GetMapping("/orders/{id}/sale")
    public ResponseEntity<CollectionModel<EntityModel<Product>>>
    productsByOrderWithSale(@PathVariable long id) {
        /*
        מצא המוצר id שהוא בתוך order מתוך orderDatabase,
        ושומר אותו ב idOrder, בזמן שלא נמצא את זה, אז תופעל הפונקציה OrderNotFoundException(תחזיר שגיאה)
        */
        Order idOrder = orderDatabase.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        // יוצר רשימה חדשה עם האובייקטים החדשים, למעשה  הם אותם המוצרים רק עם מחיר לאחר ההנחה שקבענו

        List<Product> productsAfterDiscount = new LinkedList<>();
        double priceAfterDiscount = 0.0;
        for (Product product : idOrder.getProductsList()) {
            Product tempProduct = new Product(product);
            priceAfterDiscount = tempProduct.getPrice() * 0.75;
            tempProduct.setPrice(priceAfterDiscount);
            productsAfterDiscount.add(tempProduct);
        }
        // הפכנו את -- productsAfterDiscount ל -- EntityModel ושמנו אותו ב newList ,גם הוספנו לו את הקישור המתאים לו וקישור של ה orders
        List<EntityModel<Product>> newList = productsAfterDiscount.stream()
                .map(product -> orderEntityFactory.ForProductModelWithOrderLink(idOrder, product))
                .collect(Collectors.toList());

        // נחזיר באמצעות -- ResponseEntity את ה newList וגם נוסיף סטאטוס 200
        return ResponseEntity.ok(CollectionModel.of(newList));
    }

    @DeleteMapping("/order/{id}")
    ResponseEntity<?> deleteProduct( @PathVariable(value = "id") Long orderID){
        // מוחק את המוצר לפי ה id הייחודי שלו , אשר נמצא בתוך orders מתוך -> orderDatabase
        orderDatabase.deleteById(orderID);
        // נחזיר באמצעות ResponseEntity משפט וגם מוסיף סטאטוס 200
        return ResponseEntity.ok(orderEntityFactory.toModel());
    }
}
