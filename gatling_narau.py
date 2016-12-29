import time,random
import java.lang.System

global simulation

def do_work(session):
    subtaskname = "subtask_" + session.attributes().get("requestName").get()
    for q in range(5):
        response_code = "200"
        status = "OK"
        start_time = java.lang.System.currentTimeMillis()
        sleep_time = random.randint(1,10)
        time.sleep(sleep_time)
        end_time = java.lang.System.currentTimeMillis()
        message = "DidSomeWork"+str(session.userId())
        if sleep_time % 3 == 0:
            response_code = "500"
            status = "KO"
            message = "Lost my car keys"
        simulation.genericReporter().logResponse(session, subtaskname,
                                                 start_time, end_time, status,
                                                 response_code, message, None)

def main(session):
    global simulation
    simulation = session.attributes().get("simulation").get()
    do_work(session)
    if random.randint(1,500) % 20 == 0:
        raise Exception("I don't feel like it anymore")
    return "Everybody Super Happy"
