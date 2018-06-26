package edu.cmu.ubi.simu.harlequin.util;

/**
 * Created by oscarr on 6/26/18.
 */
public class ServiceConstants {
    public final static String MOVE = "[MOVE]";
    public final static String WANDER = "[WANDER]";

    // devices
    public final static String alice_phone = "alice-phone-";
    public final static String bob_phone = "bob-phone-";
    public final static String bob_tablet = "bob-tablet-";


    // generic services
    public final static String get_self_location = "get-self-location";
    public final static String find_place_location = "find-place-location";
    public final static String get_distance_to_place = "get-distance-to-place";
    public final static String calculate_nearest_place = "calculate-nearest-place";
    public final static String share_grocery_list = "share-grocery-list";
    public final static String do_grocery_shopping = "do-grocery-shopping";
    public final static String do_beer_shopping = "do-beer-shopping";
    public final static String go_home_decor = "go-home-decor";
    public final static String go_pharmacy = "go-pharmacy";
    public final static String organize_party = "organize-party";

    // services per user
    public final static String alice_get_self_location = "alice-" + get_self_location;
    public final static String bob_get_self_location = "bob-" + get_self_location;
    public final static String alice_find_place_location = "alice-" + find_place_location;
    public final static String alice_get_distance_to_place = "alice-" + get_distance_to_place;
    public final static String bob_find_place_location = "bob-" + find_place_location;
    public final static String bob_get_distance_to_place = "bob-" + get_distance_to_place;
    public final static String cloud_calculate_nearest_place = "cloud-" + calculate_nearest_place;
    public final static String alice_share_grocery_list = "alice-" + share_grocery_list;
    public final static String bob_do_grocery_shopping = "bob-" + do_grocery_shopping;
    public final static String alice_do_grocery_shopping = "alice-" + do_grocery_shopping;
    public final static String bob_do_beer_shopping = "bob-" + do_beer_shopping;
    public final static String bob_go_home_decor = "bob-" + go_home_decor;
    public final static String bob_go_pharmacy = "bob-" + go_pharmacy;
    public final static String alice_go_home_decor = "alice-" + go_home_decor;


    // specific services
    public final static String alice_phone_get_self_location = alice_phone + get_self_location;
    public final static String bob_tablet_get_self_location = bob_tablet + get_self_location;
    public final static String alice_phone_find_place_location = alice_phone + find_place_location;
    public final static String alice_phone_get_distance_to_place = alice_phone + get_distance_to_place;
    public final static String bob_phone_find_place_location = bob_phone + find_place_location;
    public final static String bob_phone_get_distance_to_place = bob_phone + get_distance_to_place;
    public final static String alice_phone_share_grocery_list = alice_phone + share_grocery_list;
    public final static String bob_phone_do_grocery_shopping = bob_phone + do_grocery_shopping;
    public final static String alice_phone_do_grocery_shopping = alice_phone + do_grocery_shopping;
    public final static String bob_tablet_find_place_location = bob_tablet + find_place_location;
    public final static String bob_phone_do_beer_shopping = bob_phone + do_beer_shopping;
    public final static String bob_phone_go_home_decor = bob_phone + go_home_decor;
    public final static String bob_phone_go_pharmacy = bob_phone + go_pharmacy;
    public final static String bob_tablet_go_home_decor = bob_tablet + go_home_decor;
    public final static String alice_phone_go_home_decor = alice_phone + go_home_decor;

    // states
    public final static String bob_party_not_organized = "bob-party-not-organized";
    public final static String alice_party_not_organized = "alice-party-not-organized";
    public final static String bob_grocery_shopping_not_done = "bob-grocery-shopping-not-done";
    public final static String alice_grocery_shopping_not_done = "alice-grocery-shopping-not-done";
    public final static String calculate_nearest_place_required = "calculate-nearest-place-required";
    public final static String bob_distance_to_place_provided = "bob-distance-to-place-provided";
    public final static String alice_distance_to_place_provided = "alice-distance-to-place-provided";
    public final static String organize_party_done = "organize-party-done";
    public final static String bob_is_closer_to_place = "bob-is-closer-to-place";
    public final static String alice_is_closer_to_place = "alice-is-closer-to-place";
    public final static String alice_place_location_provided = "alice-place-location-provided";
    public final static String alice_place_location_required = "alice-place-location-required";
    public final static String alice_place_name_provided = "alice-place-name-provided";
    public final static String alice_close_to_organic_supermarket = "alice-close-to-organic-supermarket";
    public final static String alice_grocery_shopping_required = "alice-grocery-shopping-required";
    public final static String bob_place_location_provided = "bob-place-location-provided";
    public final static String bob_grocery_shopping_required = "bob-grocery-shopping-required";
    public final static String bob_place_location_required = "bob-place-location-required";
    public final static String bob_place_name_provided = "bob-place-name-provided";
    public final static String bob_beer_shopping_not_done = "bob-beer-shopping-not-done";
    public final static String bob_beer_shopping_required = "bob-beer-shopping-required";
    public final static String bob_driver_license_provided = "bob-driver-license-provided";
    public final static String bob_buy_decoration_required = "bob-buy-decoration-required";
    public final static String bob_somebody_has_headache = "bob-somebody-has-headache";
    public final static String bob_no_medication_at_home = "bob-no-medication-at-home";
    public final static String bob_has_coupons = "bob-has-coupons";
    public final static String alice_buy_decoration_required = "alice-buy-decoration-required";


    // goals
    public final static String grocery_shopping_done = "grocery-shopping-done";


    //users
    public final static String alice = "alice";
    public final static String bob = "bob";

}
