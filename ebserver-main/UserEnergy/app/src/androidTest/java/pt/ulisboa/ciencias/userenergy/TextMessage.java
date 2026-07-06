package pt.ulisboa.ciencias.userenergy;

public enum TextMessage {
    LONG("Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem. Nulla consequat massa quis enim. Donec pede justo, fringilla vel, aliquet nec, vulputate eget, arcu. In enim justo, rhoncus ut, imperdiet a, venenatis vitae, justo. Nullam dictum felis eu pede mollis pretium. Integer tincidu"),
    MEDIUM("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed non urna vitae elit tristique tincidunt. Aliquam erat volutpat. Vivamus sit amet magna vel libero pulvinar suscipit non et velit. Suspendisse potenti"),
    SHORT("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer nec odio vitae mauris hendrerit feugiat"),
    ACK("Yes");
    
    public final String value;

    TextMessage(String i) {
        this.value = i;
    }
}
