1. newChannel()方法调用链路
![img.png](img.png)
2. Inbound事件传播方法
![img_1.png](img_1.png)
3. OutBound事件传播方法
![img_2.png](img_2.png)
![img_3.png](img_3.png)
4.Inbound类似于事件回调(响应请求的事件)，OutBound类似于主动触发(发起请求的事件)
如果我们捕获了一个事件，并且想让这个事件传播下去，那么需要调用context的对应传播方法fireXXX()方法
5.chunk种类
![img_4.png](img_4.png)