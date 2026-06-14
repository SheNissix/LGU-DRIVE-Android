from flask import Flask, render_template, request, redirect, url_for, flash
from flask_mysqldb import MySQL
import threading
import webview
import random
import os
from datetime import date

# ========================================================
# SYSTEM CONFIGURATION & INITIALIZATION
# ========================================================
app = Flask(__name__, 
            template_folder='../templates',
            static_folder='../static')
app.secret_key = 'super_secret_session_key'

# Database Connection Parameters
app.config['MYSQL_HOST']     = 'zephyr.proxy.rlwy.net'
app.config['MYSQL_USER']     = 'root'
app.config['MYSQL_PASSWORD'] = 'aZLUGLIFlwAzyDqsEsEGIavWAqsnJaxc'
app.config['MYSQL_DB']       = 'importationform'
app.config['MYSQL_PORT']     = 37168
mysql = MySQL(app)

# ========================================================
# 1. LANDING ROUTE: INTAKE CONSOLE PLATFORM
# ========================================================
@app.route('/')
def dashboard():
    cur = mysql.connection.cursor()
    
    # Fetch distinct donors so our circular radio button lists can look up profiles
    cur.execute("SELECT DonorID, DonorName, DonorAddress, DonorTelNo, DonorFaxNo, DonorEmail FROM donor")
    donor_rows = cur.fetchall()
    donor_list = [{
        'id': d[0], 'name': d[1], 'address': d[2], 'tel': d[3], 'fax': d[4], 'email': d[5]
    } for d in donor_rows]

    # Fetch distinct donees so our circular radio button lists can look up profiles
    cur.execute("SELECT DoneeID, DoneeName, DoneeAddress, ContactPerson, DoneeTelNo, DoneeFaxNo, DoneeEmail FROM donee")
    donee_rows = cur.fetchall()
    donee_list = [{
        'id': e[0], 'name': e[1], 'address': e[2], 'contact': e[3], 'tel': e[4], 'fax': e[5], 'email': e[6]
    } for e in donee_rows]

    cur.close()
    return render_template('register_vehicle.html', donors=donor_list, donees=donee_list)

# ========================================================
# 2. TRACKING LEDGER ROUTE: CONSOLIDATED DATA HISTORY LOG
# ========================================================
@app.route('/history')
def history():
    cur = mysql.connection.cursor()
    
    # Multi-table query joining all transactional rows cleanly
    cur.execute("""
        SELECT a.ApplicationID, a.ApplicationDate, dn.DoneeName, dr.DonorName, 
               dv.DonateID, dv.VehicleDescription, dv.CarType, dv.Quantity, pc.VIN
        FROM application a
        JOIN donee dn ON a.DoneeID = dn.DoneeID
        JOIN donor dr ON a.DonorID = dr.DonorID
        JOIN donatedvehicle dv ON a.ApplicationID = dv.ApplicationID
        LEFT JOIN passengercar pc ON dv.DonateID = pc.DonateID
        ORDER BY a.ApplicationID DESC
    """)
    records = cur.fetchall()
    cur.close()
    filters = {'search': request.args.get('search', '')}
    return render_template('history.html', records=records)

# ========================================================
# 3. WORKFLOW TRANSACTION ENGINE: PARALLEL PROCESSING
# ========================================================
@app.route('/submit-vehicle', methods=['POST'])
def submit_vehicle():
    cur = mysql.connection.cursor()
    try:
        # --------------------------------------------------------
        # A. EVALUATE DONEE PROFILE IDENTITY SELECTION
        # --------------------------------------------------------
        donee_status = request.form.get('DoneeStatus', 'new')
        if donee_status == "existing" and request.form.get('ExistingDoneeID'):
            donee_id = request.form.get('ExistingDoneeID')
        else:
            donee_id = f"DON{random.randint(10, 99)}"
            d_name   = request.form['DoneeName']
            d_addr   = request.form['DoneeAddress']
            d_cont   = request.form['ContactPerson']
            d_tel    = request.form.get('DoneeTelNo', '').strip()
            d_fax    = request.form.get('DoneeFaxNo', '').strip()
            d_email  = request.form.get('DoneeEmail', '').strip()

            if not (d_tel or d_fax or d_email):
                flash("Submission rejected: New Donee entries must provide at least one contact method (Telephone, Fax, or Email).", "danger")
                return redirect(url_for('dashboard'))

            cur.execute("INSERT INTO donee VALUES (%s,%s,%s,%s,%s,%s,%s)", (donee_id, d_name, d_addr, d_cont, d_tel, d_fax, d_email))

        # --------------------------------------------------------
        # B. EVALUATE DONOR PROFILE IDENTITY SELECTION
        # --------------------------------------------------------
        donor_status = request.form.get('DonorStatus', 'new')
        if donor_status == "existing" and request.form.get('ExistingDonorID'):
            donor_id = request.form.get('ExistingDonorID')
        else:
            donor_id = f"DOR{random.randint(10, 99)}"
            dr_name  = request.form['DonorName']
            dr_addr  = request.form['DonorAddress']
            dr_tel   = request.form.get('DonorTelNo', '').strip()
            dr_fax   = request.form.get('DonorFaxNo', '').strip()
            dr_email = request.form.get('DonorEmail', '').strip()

            if not (dr_tel or dr_fax or dr_email):
                flash("Submission rejected: New Donor entries must provide at least one contact method (Telephone, Fax, or Email).", "danger")
                return redirect(url_for('dashboard'))

            cur.execute("INSERT INTO donor VALUES (%s,%s,%s,%s,%s,%s)", (donor_id, dr_name, dr_addr, dr_tel, dr_fax, dr_email))

        # --------------------------------------------------------
        # C. GENERATE AUTOMATED TRACKING INDEX SEQUENCE
        # --------------------------------------------------------
        cur.execute("SELECT COUNT(*) FROM application")
        current_count = cur.fetchone()[0]
        app_id = f"APP{current_count + 1:02d}"  

        cur.execute("INSERT INTO application VALUES (%s,%s,%s,%s,%s)", (app_id, donee_id, donor_id, date.today(), '[signature_desktop.png]'))
        vehicle_inserted = False

        # --------------------------------------------------------
        # D. INDEPENDENT LOOP: PROCESS MOTOR VEHICLES ARRAY
        # --------------------------------------------------------
        descriptions = request.form.getlist('VehicleDescription[]')
        tariffs      = request.form.getlist('VehicleTariff[]')
        quantities   = request.form.getlist('Quantity[]')
        origins      = request.form.getlist('Origin[]')

        for i in range(len(descriptions)):
            if not descriptions[i].strip():
                continue  
            
            donate_id = f"CAR{random.randint(100, 999)}"
            v_desc    = descriptions[i]
            v_tariff  = tariffs[i] if tariffs[i] else 0
            v_origin  = origins[i] if origins[i] else 'N/A'
            v_qty     = int(quantities[i]) if quantities[i] else 1

            cur.execute("INSERT INTO donatedvehicle VALUES (%s,%s,%s,%s,%s,%s,%s)", 
                        (donate_id, app_id, v_desc, v_tariff, v_origin, v_qty, "Motor Vehicle"))
            vehicle_inserted = True

        # --------------------------------------------------------
        # E. INDEPENDENT BLOCK: PROCESS PASSENGER CAR DATA ENTITY
        # --------------------------------------------------------
        p_desc = request.form.get('VehicleDescriptionSingle', '').strip()
        
        if p_desc:
            donate_id = f"CAR{random.randint(100, 999)}"
            v_tariff  = request.form.get('VehicleTariffSingle', 8703)
            v_origin  = request.form.get('OriginSingle', 'Japan')

            cur.execute("INSERT INTO donatedvehicle VALUES (%s,%s,%s,%s,%s,%s,%s)", 
                        (donate_id, app_id, p_desc, v_tariff, v_origin, 1, "Passenger Car"))
            
            vin      = request.form.get('VIN', '').upper().strip()
            y_model  = request.form.get('YearModel') if request.form.get('YearModel') else 0
            disp     = request.form.get('EngineDisplacement') if request.form.get('EngineDisplacement') else 'N/A'
            
            # FIXED: Corrected the syntax bracket issue right here
            color    = request.form.get('Color') if request.form.get('Color') else 'N/A'
            
            reg_date = request.form.get('RegistrationDate') if request.form.get('RegistrationDate') else None
            eng_no   = request.form.get('EngineNumber') if request.form.get('EngineNumber') else 'N/A'
            fuel     = request.form.get('FuelType', 'G')
            weight   = request.form.get('VehicleWeight') if request.form.get('VehicleWeight') else 0

            cur.execute("""
                INSERT INTO passengercar (VIN, DonateID, YearModel, Color, RegistrationDate, VehicleWeight, EngineNumber, EngineDisplacement, FuelType) 
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
            """, (vin, donate_id, y_model, color, reg_date, weight, eng_no, disp, fuel))
            vehicle_inserted = True

        if not vehicle_inserted:
            raise Exception("Form rejection: You must include fields for at least one asset classification.")
            
        mysql.connection.commit()
        flash(f"Application recorded successfully under system sequence identifier: {app_id}!", "success")
    except Exception as e:
        mysql.connection.rollback()
        flash(f"Database Save Interrupted: {str(e)}", "danger")
    finally:
        cur.close()
        
    return redirect(url_for('history'))

def run_flask():
    app.run(port=5000, debug=False, use_reloader=False)

if __name__ == '__main__':
    targets = [
        os.path.join("templates", "base.html"),
        os.path.join("templates", "register_vehicle.html"),
        os.path.join("templates", "history.html")
    ]
    for file_path in targets:
        if os.path.exists(file_path):
            with open(file_path, "r", encoding="utf-8") as f:
                content = f.read()
            modified = False
            for old_endpoint in ["applications", "vehicles", "passenger_cars", "donors", "donees"]:
                if f"url_for('{old_endpoint}')" in content:
                    content = content.replace(f"url_for('{old_endpoint}')", "url_for('history')")
                    modified = True
            if modified:
                with open(file_path, "w", encoding="utf-8") as f:
                    f.write(content)

    server_thread = threading.Thread(target=run_flask)
    server_thread.daemon = True
    server_thread.start()

    webview.create_window(
        title="LGU-DRIVE Importation Console Shell", 
        url="http://127.0.0.1:5000",
        width=1280,
        height=850,
        resizable=True
    )
    webview.start()
