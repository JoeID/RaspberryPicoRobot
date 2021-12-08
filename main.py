from machine import Pin, PWM, UART
import utime

'''
INITIALISATION
'''

autoMode = True

#LEDS
led_left_arm = Pin(4, Pin.OUT)
led_right_arm = Pin(16, Pin.OUT)
leds_head = Pin(6, Pin.OUT)
led_onboard = Pin(25, Pin.OUT)

#DC MOTOR
motor1a = Pin(2, Pin.OUT)
motor1b = Pin(3, Pin.OUT)

def backward():
    motor1a.high()
    motor1b.low()

def forward():
    motor1a.low()
    motor1b.high()

def stop():
    motor1a.low()
    motor1b.low()

#SERVOS
head = PWM(Pin(15))

MIN = int(5e5)
MAX = int(25e5)

left_arm = PWM(Pin(8))
right_arm = PWM(Pin(5))
hand = PWM(Pin(14))
direction = PWM(Pin(7))

left_arm.freq(50)
right_arm.freq(50)
hand.freq(50)
direction.freq(50)

def set_angle(servo, angle): #sets the angle of a MG90S servo
    tps = int((MAX-MIN)/180*angle + MIN)
    servo.duty_ns(tps)

def center_head():
    set_angle(head,78)
    utime.sleep(0.4)

def turn_head_right():
    set_angle(head,0)
    utime.sleep(0.4)

def turn_head_left():
    set_angle(head,168)
    utime.sleep(0.4)
    
def open_hand():
    set_angle(hand, 35)
    utime.sleep(0.4)

def close_hand():
    set_angle(hand, 0)
    utime.sleep(0.4)

def raise_left_arm(deg):
    set_angle(left_arm, 169-deg)
    utime.sleep(0.4)

def lower_left_arm():
    set_angle(left_arm, 169)
    utime.sleep(0.4)

def raise_right_arm(deg):
    set_angle(right_arm, deg)
    utime.sleep(0.4)

def lower_right_arm():
    set_angle(right_arm, 0)
    utime.sleep(0.4)

def say_hello():
    raise_right_arm(180)
    utime.sleep(0.4)
    open_hand()
    led_right_arm.high()
    utime.sleep(0.2)
    close_hand()
    led_right_arm.low()
    utime.sleep(0.2)
    open_hand()
    led_right_arm.high()
    utime.sleep(0.2)
    close_hand()
    led_right_arm.low()
    utime.sleep(0.2)
    open_hand()
    led_right_arm.high()
    utime.sleep(0.2)
    close_hand()
    led_right_arm.low()
    lower_right_arm()
    utime.sleep(1)

def turn_left():
    set_angle(direction,105)
    utime.sleep(0.4)

def turn_right():
    set_angle(direction,55)
    utime.sleep(0.4)

def reset_direction():
    set_angle(direction,80)
    utime.sleep(0.4)
    
#DISTANCE SENSOR
trigger = Pin(28, Pin.OUT)
echo = Pin(27, Pin.IN)

def get_distance():
    trigger.low()
    utime.sleep_us(2)
    trigger.high()
    utime.sleep_us(5)
    trigger.low()
    while echo.value() == 0:
        pass
    signaloff = utime.ticks_us()
    while echo.value() == 1:
        pass
    signalon = utime.ticks_us()
    timepassed = signalon - signaloff
    distance = (timepassed * 0.0343) / 2
    return distance

#BLUETOOTH ANTENNA
BT= UART(0,baudrate=9600)  # initialisation UART

def get_bluetooth_message():
    start = utime.time()
    msg = ""
    while utime.time() - start < 0.01:
        if BT.any():
            msg += BT.readline().decode('utf-8')
    return int(msg)

#MAIN LOOP

#head not turned = 84°

led_onboard.high()
center_head()
reset_direction()
close_hand()
lower_left_arm()
lower_right_arm()

while True:
    if not autoMode:
        while get_distance() > 45:
            forward()
            led_onboard.low()
            utime.sleep(0.05)
            led_onboard.high()
            utime.sleep(0.05)
        stop()
        say_hello()
        turn_head_left()
        utime.sleep(0.5)
        left=get_distance()
        turn_head_right()
        utime.sleep(0.5)
        right=get_distance()
        print(right,left)
        center_head()
        
        if right>left:
            print("I'm turning right")
            turn_right()
            forward()
            utime.sleep(1)
            stop()
            turn_left()
            backward()
            utime.sleep(1)
            stop()
            turn_right()
            forward()
            utime.sleep(1)
            reset_direction()
        else:
            print("I'm turning left")
            turn_left()
            forward()
            utime.sleep(1)
            stop()
            turn_right()
            backward()
            utime.sleep(1)
            stop()
            turn_left()
            forward()
            utime.sleep(1)
            reset_direction()
    else:
        if BT.any():
            order = get_bluetooth_message()
            print(order)
            
            if order == 0:
                leds_head.low()
            elif order == 1:
                leds_head.high()
            elif order == 2:
                led_left_arm.low()
            elif order == 3:
                led_left_arm.high()
            elif order == 4:
                led_right_arm.low()
            elif order == 5:
                led_right_arm.high()
            elif order == 6:
                forward()
            elif order == 7:
                backward()
            elif order == 8:
                stop()
            elif order == 9:
                turn_left()
            elif order == 10:
                turn_right()
            elif order == 11:
                reset_direction()
            elif 20 <= order <= 200:
                raise_left_arm(order-20)
            elif 220 <= order <= 400:
                raise_right_arm(order-220)
            elif 420 <= order <= 600:
                set_angle(hand,order-420)
            elif 620 <= order <= 800:
                set_angle(head,order-620)
                
