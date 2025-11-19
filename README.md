# signshop
The all-in-one player shop solution for fabric servers. Idea based off 
chestshop for bukkit.

Still in alpha, technical documentation in the works.

Document index:
- 1 Player guide
- 2 Admin guide
  - 2.1 First setup
  - 2.2 Configuration
    - Message prefix
- 3 Technical documentation

# Player guide
> [!TIP]
> This section of the README contains a tutorial on how to use the mod as a 
> player, if you are looking for the admin guide, go down to the ***admin 
> guide*** section of the README.

To create a signshop hold a **redstone dust** in your main hand. Click on the 
container containing the items, then click on the chest containing the price.
Container containing the price at first must contain the item and the amount of
item(s) you plan to charge. The item container must contain the item you plan 
on selling and the amount of items you plan to sell.

After you have clicked on both the item and price container click on a sign 
that has `[trade]` as its first line. Sign has to have `[trade]` as its first 
line, the rest can have whatever text you want.

*As of 0.0.2-ALPHA* signshop supports every storage block. e.g. Chests, hoppers
shulkerboxes, barrels, trapped chests.

# Admin guide
This section of the README goes over administration when using this plugin.
## First setup
This plugin requires a database connection, create a database and set the 
parameters in the config `config/signshop/config.properties`. Admin commands 
coming soon.

```properties
database.password=password
database.url=jdbc\:mysql\://localhost\:3000/signshop
database.user=user
```

## Configuration
Non-essential plugin configuration.
### Message prefix
You can change the prefix that is added to messages this plugin gives to
players and admins. In an example message `signshop >> Shop created 
sucsessfully!`, the prefix is `signshop >> `.
```properties
prefix=§2§lsignshop§r >> 
```
The prefix can be disabled by simply leaving this as an empty string.
```properties
prefix=
```

# Technical documentation
This section of the README goes over the technical inner workings of the
signshop mod.
> [!NOTE]
> This part of the text is still being written.
## Method of operation
Signshop operates signs using the `SignEventListener`, it listens to:
- block break events
- block attack events
- block use events

## Nomenclature
All mod code is situated in main *(server side module)*, packages all are 
situated in `com.lukeonuke`. They are separated as follows:
- `.event` - Houses event listeners, currently only `SignEventListener`
- `.mixin` - Isn't used, likely to be removed.
- `.model` - Houses database data models.
  - `.model.nondb` - Houses models for data that isn't stored in the database.
- `.service` - Houses services.

### Service class naming
Classes that end in `Util`, e.g. `ShopUtil` or `InventoryUtil` house static 
methods. Classes that end in `Service` are usually singletons.

## Filesystem usage
Only file this mod operates directly is stored in `/config/signshop` directory.
The said file is the configuration file *(see: section "Configuration" of the 
admin guide)* *(see: section "First setup" of the admin guide)* 
`config.properties`.

## Why caching isn't used?
It the mods intended use case, where the database is hosted on the same machine
or in the same building as the game server, the delay between a request and a
response is so little that there is no need to add additional code to cache
and validate the cache.



