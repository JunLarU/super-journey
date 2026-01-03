package core.data.Ingredientes;

public class CategoriaIngrediente {

    private final int id;
    private final String nombre;

    public CategoriaIngrediente(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public String toString() {
        return nombre; // 🔥 Esto es lo que se muestra en el ComboBox
    }
}
