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
> This section of the readme contains a tutorial on how to use the mod as a 
> player, if you are looking for the admin guide, go down to the ***admin 
> guide*** section of the README.

To create a signshop hold a **redstone dust** in your main hand. Click on the 
chest containing the items, then click on the chest containing the price.
Chest containing the price at first must contain the item and the amount of
item(s) you plan to sell. The price chest must also contain the item you plan
on using as currency and in an amount you plan to charge.

After you have clicked on both the item and price chests click on a sign that
has `[trade]` as its first line. Sign has to have `[trade]` as its first line, 
the rest can have whatever text you want.

*Currently only chests work, although it is planned to make all containers work
in the future.*

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
> [!NOTE]
> This section of the readme contains a tutorial on how to use the mod as a
> player, if you are looking for the admin guide, go down to the ***admin
> guide*** section of the README.
This section of the README goes over the technical inner workings of the 
signshop mod.



