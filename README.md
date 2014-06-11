SimpleClient
============

## Table of contents

 * __[Using SimpleClient](#usage)__
 * __[Noted and unverifiable bugs](#bugs)__

## <a name="usage"></a>Using SimpleClient

Using SimpleClient in your code can be as simple and painless as:

```java
new SimpleClient("1.7.9", "username", "password", new File("%APPDATA%")).openMinecraft();
```

Of course, there are a couple of things outside of this to be aware of, but that's the main thing really.

There are two ways to construct a SimpleClient instance. Once is supplying the four necessary fields
directly, as shown above. The other involves passing a scanner instance for requesting input. This can
be done with `stdin` as shown below:

```java
public static void main(String[] args) throws IOException {
    Scanner scan = new Scanner(System.in);
    new SimpleClient(scan).openMinecraft();
    System.exit(0);
}
```
And that is really the jist of it all.

**Do note, however**, that opening the minecraft client creates a subproccess and that your main process
(the one launching the client) will not close unless you specifically call `System#exit(int)`.

## <a name="bugs"></a>Noted and unverifiable bugs

* Skins don't always show up
* The client logo is different
