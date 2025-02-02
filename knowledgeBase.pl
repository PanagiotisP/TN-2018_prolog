validHighway(motorway).
validHighway(trunk).
validHighway(primary).
validHighway(secondary).
validHighway(tertiary).
validHighway(unclassified).
validHighway(residential).
validHighway(motorway_link).
validHighway(trunk_link).
validHighway(primary_link).
validHighway(secondary_link).
validHighway(tertiary_link).
validHighway(living_street).

validAccessibility(yes).
validAccessibility(permissive).
validAccessibility(destination).
validAccessibility(null).
validAccessibility(allowed).
drivable(Highway, Access) :- validAccessibility(Access), validHighway(Highway).

validPairing(X) :- taxi(_, _, X, yes, MaxN, TaxiLangs, _), client(_, _, _, _, _, Person, ClientLang), Person =< MaxN, member(ClientLang, TaxiLangs).
direction(Oneway, Res) :- Oneway = yes -> Res = 1 ; (Oneway = -1 -> Res = -1 ; Res = 0).
